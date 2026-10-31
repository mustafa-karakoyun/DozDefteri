package app.dozdefteri.data.repository

import androidx.room.withTransaction
import app.dozdefteri.data.db.AppDatabase
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.StockLogEntity
import app.dozdefteri.domain.DoseGenerator
import app.dozdefteri.domain.StockEstimator
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.OPEN_DOSE_STATUSES
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.domain.model.StockChangeReason
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Single source of truth for dose events and the stock changes they cause. */
@Singleton
class DoseRepository @Inject constructor(
    private val db: AppDatabase,
    private val generator: DoseGenerator,
    private val clock: Clock,
    private val lowStockNotifier: LowStockNotifier
) {
    private val doseDao = db.doseEventDao()
    private val medicationDao = db.medicationDao()
    private val scheduleDao = db.scheduleDao()
    private val stockLogDao = db.stockLogDao()

    fun observeRange(profileId: Long?, from: Instant, to: Instant): Flow<List<DoseEventDetails>> =
        doseDao.observeDetailsRange(profileId, from.toEpochMilli(), to.toEpochMilli())

    suspend fun getRange(profileId: Long?, from: Instant, to: Instant): List<DoseEventDetails> =
        doseDao.getDetailsRange(profileId, from.toEpochMilli(), to.toEpochMilli())

    suspend fun getDetails(eventId: Long): DoseEventDetails? = doseDao.getDetailsById(eventId)

    suspend fun nextOpenForProfile(profileId: Long): DoseEventDetails? = doseDao.getNextOpenForProfile(profileId)

    /** Earliest `scheduledAt` of an open dose that has not been notified yet. */
    suspend fun nextUnnotifiedTime(): Long? = doseDao.nextUnnotifiedTime()

    /** Open, not yet notified doses scheduled at or before [limit]. */
    suspend fun dueUnnotified(limit: Instant): List<DoseEventDetails> = doseDao.getDueUnnotified(limit.toEpochMilli())

    suspend fun markNotified(eventIds: List<Long>, now: Instant) {
        if (eventIds.isNotEmpty()) doseDao.markNotified(eventIds, now.toEpochMilli())
    }

    /**
     * Creates the dose events of every non-archived schedule in `[now - 1 min, now + 48 h)`.
     * Existing events are kept untouched thanks to the unique `(scheduleId, originalScheduledAt)` index.
     */
    suspend fun ensureWindow(now: Instant) {
        val from = now.minus(WINDOW_PAST)
        val to = now.plus(WINDOW_FUTURE)
        val zone = clock.zone
        val events = scheduleDao.getActive().flatMap { active ->
            generator.generate(active.schedule, from, to, zone).map { instant ->
                val millis = instant.toEpochMilli()
                DoseEventEntity(
                    scheduleId = active.schedule.id,
                    medicationId = active.schedule.medicationId,
                    profileId = active.profileId,
                    originalScheduledAt = millis,
                    scheduledAt = millis,
                    status = DoseStatus.PENDING,
                    notifiedAt = null,
                    actionAt = null,
                    quantity = active.doseAmount
                )
            }
        }
        if (events.isNotEmpty()) doseDao.insertIgnore(events)
    }

    /**
     * Called after a schedule or its medication changed: removes the schedule's future PENDING/SNOOZED events and
     * re-creates the window. Past events are never touched.
     */
    suspend fun regenerateForSchedule(scheduleId: Long, now: Instant) {
        db.withTransaction {
            doseDao.deleteFutureOpenForSchedule(scheduleId, now.toEpochMilli())
        }
        ensureWindow(now)
    }

    /**
     * Marks PENDING/SNOOZED events scheduled before `now - missedAfterMinutes` as MISSED.
     * Returns the ids of the events that changed so their notifications can be cancelled.
     */
    suspend fun markMissed(now: Instant, missedAfterMinutes: Int): List<Long> = db.withTransaction {
        val threshold = now.minus(Duration.ofMinutes(missedAfterMinutes.toLong())).toEpochMilli()
        val ids = doseDao.getOverdueOpenIds(threshold)
        if (ids.isNotEmpty()) doseDao.markOverdueMissed(threshold, now.toEpochMilli())
        ids
    }

    /** Marks an open event as TAKEN and deducts its quantity from the stock. Returns false if nothing changed. */
    suspend fun take(eventId: Long, now: Instant): Boolean {
        val lowStock = db.withTransaction {
            val event = doseDao.getById(eventId)
            if (event == null || event.status !in OPEN_DOSE_STATUSES) return@withTransaction NOT_APPLIED
            doseDao.update(event.copy(status = DoseStatus.TAKEN, actionAt = now.toEpochMilli()))
            TakeOutcome(applied = true, lowStock = deductStock(event.medicationId, event.quantity, now))
        }
        lowStock.lowStock?.let { notifyLowStock(it) }
        return lowStock.applied
    }

    /** Records an AS_NEEDED dose taken right now. Returns false if the medication has no AS_NEEDED schedule. */
    suspend fun takeAsNeeded(medicationId: Long, now: Instant): Boolean {
        val outcome = db.withTransaction {
            val medication = medicationDao.getById(medicationId) ?: return@withTransaction NOT_APPLIED
            val schedule = scheduleDao.getByMedication(medicationId).firstOrNull { it.type == ScheduleType.AS_NEEDED }
                ?: return@withTransaction NOT_APPLIED
            val millis = now.toEpochMilli()
            val inserted = doseDao.insertIgnore(
                listOf(
                    DoseEventEntity(
                        scheduleId = schedule.id,
                        medicationId = medication.id,
                        profileId = medication.profileId,
                        originalScheduledAt = millis,
                        scheduledAt = millis,
                        status = DoseStatus.TAKEN,
                        notifiedAt = null,
                        actionAt = millis,
                        quantity = medication.doseAmount
                    )
                )
            )
            if (inserted.firstOrNull() == -1L) return@withTransaction NOT_APPLIED
            TakeOutcome(applied = true, lowStock = deductStock(medication.id, medication.doseAmount, now))
        }
        outcome.lowStock?.let { notifyLowStock(it) }
        return outcome.applied
    }

    suspend fun skip(eventId: Long, now: Instant): Boolean = db.withTransaction {
        val event = doseDao.getById(eventId)
        if (event == null || event.status !in OPEN_DOSE_STATUSES) return@withTransaction false
        doseDao.update(event.copy(status = DoseStatus.SKIPPED, actionAt = now.toEpochMilli()))
        true
    }

    suspend fun snooze(eventId: Long, now: Instant, minutes: Int): Boolean = db.withTransaction {
        val event = doseDao.getById(eventId)
        if (event == null || event.status !in OPEN_DOSE_STATUSES) return@withTransaction false
        doseDao.update(
            event.copy(
                status = DoseStatus.SNOOZED,
                scheduledAt = now.plus(Duration.ofMinutes(minutes.toLong())).toEpochMilli(),
                notifiedAt = null
            )
        )
        true
    }

    /**
     * Reverts a TAKEN or SKIPPED event whose action happened within the last 24 hours. A reverted TAKEN dose
     * returns its quantity to the stock. Scheduled doses go back to PENDING; AS_NEEDED records are removed.
     */
    suspend fun undo(eventId: Long, now: Instant): Boolean = db.withTransaction {
        val event = doseDao.getById(eventId) ?: return@withTransaction false
        val actionAt = event.actionAt ?: return@withTransaction false
        if (event.status != DoseStatus.TAKEN && event.status != DoseStatus.SKIPPED) return@withTransaction false
        if (now.toEpochMilli() - actionAt > UNDO_WINDOW.toMillis()) return@withTransaction false

        if (event.status == DoseStatus.TAKEN) refundStock(event.medicationId, event.quantity, now)
        val schedule = scheduleDao.getById(event.scheduleId)
        if (schedule?.type == ScheduleType.AS_NEEDED) {
            doseDao.deleteById(event.id)
        } else {
            doseDao.update(event.copy(status = DoseStatus.PENDING, actionAt = null))
        }
        true
    }

    /**
     * Records a dose as taken at [takenAt] (from the "Güncelle" / "Saati düzelt" sheet). Works for any status;
     * stock is only deducted when the dose was not already TAKEN. Returns false for an unknown event.
     */
    suspend fun markTakenAt(eventId: Long, takenAt: Instant, now: Instant): Boolean {
        val outcome = db.withTransaction {
            val event = doseDao.getById(eventId) ?: return@withTransaction NOT_APPLIED
            val wasTaken = event.status == DoseStatus.TAKEN
            doseDao.update(event.copy(status = DoseStatus.TAKEN, actionAt = takenAt.toEpochMilli()))
            val lowStock = if (wasTaken) null else deductStock(event.medicationId, event.quantity, now)
            TakeOutcome(applied = true, lowStock = lowStock)
        }
        outcome.lowStock?.let { notifyLowStock(it) }
        return outcome.applied
    }

    /** Marks a dose as skipped / not taken; a TAKEN dose returns its quantity to the stock. */
    suspend fun markSkipped(eventId: Long, now: Instant): Boolean = db.withTransaction {
        val event = doseDao.getById(eventId) ?: return@withTransaction false
        if (event.status == DoseStatus.SKIPPED) return@withTransaction false
        if (event.status == DoseStatus.TAKEN) refundStock(event.medicationId, event.quantity, now)
        doseDao.update(event.copy(status = DoseStatus.SKIPPED, actionAt = now.toEpochMilli()))
        true
    }

    suspend fun lastTakenAt(medicationId: Long): Instant? =
        doseDao.lastTakenAt(medicationId)?.let(Instant::ofEpochMilli)

    /**
     * Archives the medication and deletes its future PENDING/SNOOZED events.
     * Returns the ids of its currently shown (notified, still open) doses so their notifications can be cancelled.
     */
    suspend fun archiveMedication(medicationId: Long, now: Instant): List<Long> = db.withTransaction {
        val medication = medicationDao.getById(medicationId) ?: return@withTransaction emptyList()
        medicationDao.update(medication.copy(isArchived = true, updatedAt = now.toEpochMilli()))
        doseDao.deleteFutureOpenForMedication(medicationId, now.toEpochMilli())
        doseDao.getNotifiedOpenIdsForMedication(medicationId)
    }

    private suspend fun notifyLowStock(medication: MedicationEntity) {
        val days = StockEstimator.remainingDays(
            medication.stockQuantity,
            scheduleDao.getByMedication(medication.id),
            medication.doseAmount
        )
        lowStockNotifier.notifyLowStock(medication, days)
    }

    /** Returns the medication when it crossed its low-stock threshold and the alert has not been shown yet. */
    private suspend fun deductStock(medicationId: Long, quantity: Double, now: Instant): MedicationEntity? {
        val medication = medicationDao.getById(medicationId) ?: return null
        val stock = medication.stockQuantity ?: return null
        val newStock = (stock - quantity).coerceAtLeast(0.0)
        val delta = newStock - stock
        if (delta != 0.0) {
            stockLogDao.insert(
                StockLogEntity(
                    medicationId = medicationId,
                    delta = delta,
                    reason = StockChangeReason.DOSE_TAKEN,
                    createdAt = now.toEpochMilli()
                )
            )
        }
        val threshold = medication.lowStockThreshold
        val crossed = threshold != null && newStock <= threshold && !medication.lowStockNotified
        val updated = medication.copy(
            stockQuantity = newStock,
            lowStockNotified = medication.lowStockNotified || crossed,
            updatedAt = now.toEpochMilli()
        )
        medicationDao.update(updated)
        return if (crossed) updated else null
    }

    private suspend fun refundStock(medicationId: Long, quantity: Double, now: Instant) {
        val medication = medicationDao.getById(medicationId) ?: return
        val stock = medication.stockQuantity ?: return
        val newStock = stock + quantity
        stockLogDao.insert(
            StockLogEntity(
                medicationId = medicationId,
                delta = quantity,
                reason = StockChangeReason.DOSE_UNDONE,
                createdAt = now.toEpochMilli()
            )
        )
        val threshold = medication.lowStockThreshold
        medicationDao.update(
            medication.copy(
                stockQuantity = newStock,
                lowStockNotified = medication.lowStockNotified && (threshold == null || newStock <= threshold),
                updatedAt = now.toEpochMilli()
            )
        )
    }

    private data class TakeOutcome(val applied: Boolean, val lowStock: MedicationEntity?)

    companion object {
        val WINDOW_PAST: Duration = Duration.ofMinutes(1)
        val WINDOW_FUTURE: Duration = Duration.ofHours(48)
        val UNDO_WINDOW: Duration = Duration.ofHours(24)
        private val NOT_APPLIED = TakeOutcome(applied = false, lowStock = null)
    }
}
