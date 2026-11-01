package app.dozdefteri.data.repository

import androidx.room.withTransaction
import app.dozdefteri.data.db.AppDatabase
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.data.db.entity.StockLogEntity
import app.dozdefteri.domain.StockEstimator
import app.dozdefteri.domain.model.StockChangeReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class MedicationWithSchedule(val medication: MedicationEntity, val schedule: ScheduleEntity?)

@Singleton
class MedicationRepository @Inject constructor(
    private val db: AppDatabase,
    private val doseRepository: DoseRepository
) {
    private val medicationDao = db.medicationDao()
    private val scheduleDao = db.scheduleDao()
    private val stockLogDao = db.stockLogDao()

    fun observeMedications(profileId: Long, archived: Boolean): Flow<List<MedicationEntity>> =
        medicationDao.observeByProfile(profileId, archived)

    fun observeSchedules(): Flow<List<ScheduleEntity>> = scheduleDao.observeAll()

    /** Active medication count per profile id. */
    fun observeActiveCounts(): Flow<Map<Long, Int>> =
        medicationDao.observeActiveCounts().map { counts -> counts.associate { it.profileId to it.count } }

    suspend fun get(medicationId: Long): MedicationWithSchedule? {
        val medication = medicationDao.getById(medicationId) ?: return null
        return MedicationWithSchedule(medication, scheduleDao.getByMedication(medicationId).firstOrNull())
    }

    suspend fun count(): Int = medicationDao.count()

    /** Non-archived medications with their schedule, of one profile or of all profiles when [profileId] is null. */
    suspend fun getActiveWithSchedules(profileId: Long?): List<MedicationWithSchedule> {
        val schedules = scheduleDao.getAll().groupBy { it.medicationId }
        return medicationDao.getAll()
            .filter { !it.isArchived && (profileId == null || it.profileId == profileId) }
            .sortedBy { it.name.lowercase() }
            .map { MedicationWithSchedule(it, schedules[it.id]?.firstOrNull()) }
    }

    /**
     * Inserts or updates a medication with its single schedule (MVP UI manages one schedule per medication),
     * records manual stock changes and regenerates future doses. Returns the medication id.
     */
    suspend fun save(medication: MedicationEntity, schedule: ScheduleEntity, now: Instant): Long {
        val millis = now.toEpochMilli()
        val (medicationId, scheduleId) = db.withTransaction {
            if (medication.id == 0L) {
                val id = medicationDao.insert(medication.copy(createdAt = millis, updatedAt = millis))
                medication.stockQuantity?.takeIf { it != 0.0 }?.let { quantity ->
                    stockLogDao.insert(
                        StockLogEntity(
                            medicationId = id,
                            delta = quantity,
                            reason = StockChangeReason.MANUAL_SET,
                            createdAt = millis
                        )
                    )
                }
                val scheduleId = scheduleDao.insert(schedule.copy(id = 0, medicationId = id))
                id to scheduleId
            } else {
                val old = medicationDao.getById(medication.id)
                val stockChanged = old != null && old.stockQuantity != medication.stockQuantity
                val newStock = medication.stockQuantity
                val threshold = medication.lowStockThreshold
                val resetLowStock = newStock == null || threshold == null || newStock > threshold
                medicationDao.update(
                    medication.copy(
                        createdAt = old?.createdAt ?: millis,
                        updatedAt = millis,
                        isArchived = old?.isArchived ?: false,
                        lowStockNotified = if (stockChanged && resetLowStock) false else medication.lowStockNotified
                    )
                )
                if (stockChanged && newStock != null) {
                    stockLogDao.insert(
                        StockLogEntity(
                            medicationId = medication.id,
                            delta = newStock - (old?.stockQuantity ?: 0.0),
                            reason = StockChangeReason.MANUAL_SET,
                            createdAt = millis
                        )
                    )
                }
                val existing = scheduleDao.getByMedication(medication.id).firstOrNull()
                val scheduleId = if (existing == null) {
                    scheduleDao.insert(schedule.copy(id = 0, medicationId = medication.id))
                } else {
                    scheduleDao.update(schedule.copy(id = existing.id, medicationId = medication.id))
                    existing.id
                }
                medication.id to scheduleId
            }
        }
        doseRepository.regenerateForSchedule(scheduleId, now)
        return medicationId
    }

    /** Adds [amount] to the stock (MANUAL_ADD) and clears the low-stock flag when back above the threshold. */
    suspend fun addStock(medicationId: Long, amount: Double, now: Instant) = db.withTransaction {
        val medication = medicationDao.getById(medicationId) ?: return@withTransaction
        val newStock = (medication.stockQuantity ?: 0.0) + amount
        val threshold = medication.lowStockThreshold
        medicationDao.update(
            medication.copy(
                stockQuantity = newStock,
                lowStockNotified = medication.lowStockNotified && threshold != null && newStock <= threshold,
                updatedAt = now.toEpochMilli()
            )
        )
        stockLogDao.insert(
            StockLogEntity(
                medicationId = medicationId,
                delta = amount,
                reason = StockChangeReason.MANUAL_ADD,
                createdAt = now.toEpochMilli()
            )
        )
    }

    /**
     * "Stok girilmemiş · ekle": starts stock tracking with [amount] and a [lowStockDays] alert, converting the days
     * into an amount threshold with the schedule's daily average.
     */
    suspend fun startStockTracking(medicationId: Long, amount: Double, lowStockDays: Int, now: Instant) {
        db.withTransaction {
            val medication = medicationDao.getById(medicationId) ?: return@withTransaction
            val schedules = scheduleDao.getByMedication(medicationId)
            val average = StockEstimator.dailyAverage(schedules, medication.doseAmount)
            medicationDao.update(
                medication.copy(
                    stockQuantity = amount,
                    lowStockDays = lowStockDays,
                    lowStockThreshold = if (average > 0.0) lowStockDays * average else null,
                    lowStockNotified = false,
                    updatedAt = now.toEpochMilli()
                )
            )
            stockLogDao.insert(
                StockLogEntity(
                    medicationId = medicationId,
                    delta = amount,
                    reason = StockChangeReason.MANUAL_SET,
                    createdAt = now.toEpochMilli()
                )
            )
        }
    }

    /** See [DoseRepository.archiveMedication]. */
    suspend fun archive(medicationId: Long, now: Instant): List<Long> =
        doseRepository.archiveMedication(medicationId, now)

    suspend fun unarchive(medicationId: Long, now: Instant) {
        val medication = medicationDao.getById(medicationId) ?: return
        medicationDao.update(medication.copy(isArchived = false, updatedAt = now.toEpochMilli()))
        doseRepository.ensureWindow(now)
    }
}
