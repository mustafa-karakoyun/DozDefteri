package app.dozdefteri.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.dozdefteri.data.db.AppDatabase
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.LowStockNotifier
import app.dozdefteri.domain.DoseGenerator
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.domain.model.StockChangeReason
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DoseRepositoryTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val now: Instant = LocalDateTime.parse("2026-11-02T07:00").atZone(zone).toInstant()
    private val clock = Clock.fixed(now, zone)

    private lateinit var db: AppDatabase
    private lateinit var repository: DoseRepository
    private val lowStockAlerts = mutableListOf<MedicationEntity>()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = DoseRepository(
            db,
            DoseGenerator(),
            clock,
            LowStockNotifier { medication, _ -> lowStockAlerts += medication }
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertMedication(
        stock: Double? = null,
        threshold: Double? = null,
        doseAmount: Double = 1.0,
        type: ScheduleType = ScheduleType.DAILY,
        times: List<String> = listOf("08:00", "20:00")
    ): Pair<Long, Long> {
        val profileId = db.profileDao().insert(testProfile())
        val medicationId = db.medicationDao().insert(
            testMedication(profileId, stock = stock, threshold = threshold, doseAmount = doseAmount)
        )
        val scheduleId = db.scheduleDao().insert(testSchedule(medicationId, type = type, times = times))
        return medicationId to scheduleId
    }

    private suspend fun firstEventId(): Long = db.doseEventDao().getAll().minBy { it.scheduledAt }.id

    @Test
    fun ensureWindow_calledTwice_doesNotDuplicate() = runTest {
        insertMedication()
        repository.ensureWindow(now)
        val first = db.doseEventDao().getAll()
        repository.ensureWindow(now)
        repository.ensureWindow(now.plusSeconds(30))
        val second = db.doseEventDao().getAll()
        // 48 hours from 07:00 with 08:00 and 20:00 doses -> 4 doses.
        assertThat(first).hasSize(4)
        assertThat(second).isEqualTo(first)
    }

    @Test
    fun regenerate_doesNotTouchPastEvents() = runTest {
        val (_, scheduleId) = insertMedication()
        repository.ensureWindow(now)
        val firstId = firstEventId()
        val later = now.plus(Duration.ofHours(2)) // 09:00, after the 08:00 dose
        repository.take(firstId, later)

        repository.regenerateForSchedule(scheduleId, later)

        val events = db.doseEventDao().getBySchedule(scheduleId)
        val past = events.single { it.id == firstId }
        assertThat(past.status).isEqualTo(DoseStatus.TAKEN)
        assertThat(events.count { it.status == DoseStatus.PENDING }).isAtLeast(3)
        assertThat(events.map { it.originalScheduledAt }).containsNoDuplicates()
    }

    @Test
    fun markMissed_respectsThresholdBoundary() = runTest {
        insertMedication(times = listOf("08:00"))
        repository.ensureWindow(now)
        val dose = db.doseEventDao().getAll().minBy { it.scheduledAt }
        val doseTime = Instant.ofEpochMilli(dose.scheduledAt)

        // Exactly at the threshold: scheduledAt == now - 180 min is not "before" the threshold.
        val ids = repository.markMissed(doseTime.plus(Duration.ofMinutes(180)), 180)
        assertThat(ids).isEmpty()
        assertThat(db.doseEventDao().getById(dose.id)?.status).isEqualTo(DoseStatus.PENDING)

        val missedIds = repository.markMissed(doseTime.plus(Duration.ofMinutes(180)).plusMillis(1), 180)
        assertThat(missedIds).containsExactly(dose.id)
        val missed = db.doseEventDao().getById(dose.id)
        assertThat(missed?.status).isEqualTo(DoseStatus.MISSED)
        assertThat(missed?.actionAt).isNotNull()
    }

    @Test
    fun take_decreasesStock_andNeverBelowZero() = runTest {
        val (medicationId, _) = insertMedication(stock = 1.5, doseAmount = 1.0)
        repository.ensureWindow(now)
        val events = db.doseEventDao().getAll().sortedBy { it.scheduledAt }

        assertThat(repository.take(events[0].id, now)).isTrue()
        assertThat(db.medicationDao().getById(medicationId)?.stockQuantity).isEqualTo(0.5)

        assertThat(repository.take(events[1].id, now)).isTrue()
        assertThat(db.medicationDao().getById(medicationId)?.stockQuantity).isEqualTo(0.0)

        val logs = db.stockLogDao().getByMedication(medicationId)
        assertThat(logs.map { it.reason }).containsExactly(StockChangeReason.DOSE_TAKEN, StockChangeReason.DOSE_TAKEN)
        assertThat(logs.map { it.delta }).containsExactly(-1.0, -0.5).inOrder()

        // A second take on the same event is ignored.
        assertThat(repository.take(events[0].id, now)).isFalse()
    }

    @Test
    fun lowStockFlag_isSetAndNotifiedOnce() = runTest {
        val (medicationId, _) = insertMedication(stock = 3.0, threshold = 2.0)
        repository.ensureWindow(now)
        val events = db.doseEventDao().getAll().sortedBy { it.scheduledAt }

        repository.take(events[0].id, now) // 2.0 <= 2.0 -> alert
        repository.take(events[1].id, now) // 1.0 -> already notified
        assertThat(lowStockAlerts).hasSize(1)
        assertThat(db.medicationDao().getById(medicationId)?.lowStockNotified).isTrue()
    }

    @Test
    fun undo_returnsStock() = runTest {
        val (medicationId, _) = insertMedication(stock = 10.0)
        repository.ensureWindow(now)
        val eventId = firstEventId()
        repository.take(eventId, now)
        assertThat(db.medicationDao().getById(medicationId)?.stockQuantity).isEqualTo(9.0)

        assertThat(repository.undo(eventId, now.plusSeconds(60))).isTrue()

        assertThat(db.medicationDao().getById(medicationId)?.stockQuantity).isEqualTo(10.0)
        val event = db.doseEventDao().getById(eventId)
        assertThat(event?.status).isEqualTo(DoseStatus.PENDING)
        assertThat(event?.actionAt).isNull()
        assertThat(db.stockLogDao().getByMedication(medicationId).last().reason)
            .isEqualTo(StockChangeReason.DOSE_UNDONE)
    }

    @Test
    fun undo_after24Hours_isRejected() = runTest {
        insertMedication(stock = 10.0)
        repository.ensureWindow(now)
        val eventId = firstEventId()
        repository.skip(eventId, now)
        assertThat(repository.undo(eventId, now.plus(Duration.ofHours(25)))).isFalse()
        assertThat(db.doseEventDao().getById(eventId)?.status).isEqualTo(DoseStatus.SKIPPED)
    }

    @Test
    fun snooze_movesScheduledAt_andClearsNotifiedAt() = runTest {
        insertMedication()
        repository.ensureWindow(now)
        val eventId = firstEventId()
        db.doseEventDao().markNotified(listOf(eventId), now.toEpochMilli())
        val original = db.doseEventDao().getById(eventId)

        val snoozeAt = now.plus(Duration.ofHours(1))
        assertThat(repository.snooze(eventId, snoozeAt, 10)).isTrue()

        val snoozed = db.doseEventDao().getById(eventId)
        assertThat(snoozed?.status).isEqualTo(DoseStatus.SNOOZED)
        assertThat(snoozed?.scheduledAt).isEqualTo(snoozeAt.plus(Duration.ofMinutes(10)).toEpochMilli())
        assertThat(snoozed?.notifiedAt).isNull()
        assertThat(snoozed?.originalScheduledAt).isEqualTo(original?.originalScheduledAt)
    }

    @Test
    fun takeAsNeeded_insertsTakenEvent() = runTest {
        val (medicationId, scheduleId) =
            insertMedication(type = ScheduleType.AS_NEEDED, times = emptyList(), stock = 5.0)
        repository.ensureWindow(now)
        assertThat(db.doseEventDao().getAll()).isEmpty()

        assertThat(repository.takeAsNeeded(medicationId, now)).isTrue()

        val event = db.doseEventDao().getAll().single()
        assertThat(event.scheduleId).isEqualTo(scheduleId)
        assertThat(event.status).isEqualTo(DoseStatus.TAKEN)
        assertThat(event.originalScheduledAt).isEqualTo(now.toEpochMilli())
        assertThat(event.actionAt).isEqualTo(now.toEpochMilli())
        assertThat(db.medicationDao().getById(medicationId)?.stockQuantity).isEqualTo(4.0)
    }

    @Test
    fun archive_removesFutureOpenEvents() = runTest {
        val (medicationId, _) = insertMedication()
        repository.ensureWindow(now)
        assertThat(db.doseEventDao().getAll()).isNotEmpty()

        repository.archiveMedication(medicationId, now)

        assertThat(db.doseEventDao().getAll()).isEmpty()
        assertThat(db.medicationDao().getById(medicationId)?.isArchived).isTrue()
        repository.ensureWindow(now)
        assertThat(db.doseEventDao().getAll()).isEmpty()
    }

    @Test
    fun markTakenAt_onMissedDose_setsTimeAndDeductsStockOnce() = runTest {
        val (medicationId, _) = insertMedication(stock = 10.0)
        repository.ensureWindow(now)
        val eventId = firstEventId()
        val takenAt = now.plus(Duration.ofMinutes(65))
        repository.markMissed(now.plus(Duration.ofHours(5)), 60)

        assertThat(repository.markTakenAt(eventId, takenAt, now.plus(Duration.ofHours(5)))).isTrue()
        assertThat(repository.markTakenAt(eventId, takenAt.plusSeconds(60), now.plus(Duration.ofHours(5)))).isTrue()

        val event = db.doseEventDao().getById(eventId)
        assertThat(event?.status).isEqualTo(DoseStatus.TAKEN)
        assertThat(event?.actionAt).isEqualTo(takenAt.plusSeconds(60).toEpochMilli())
        assertThat(db.medicationDao().getById(medicationId)?.stockQuantity).isEqualTo(9.0)
        assertThat(repository.lastTakenAt(medicationId)).isEqualTo(takenAt.plusSeconds(60))
    }

    @Test
    fun markSkipped_onTakenDose_refundsStock() = runTest {
        val (medicationId, _) = insertMedication(stock = 10.0)
        repository.ensureWindow(now)
        val eventId = firstEventId()
        repository.take(eventId, now)

        assertThat(repository.markSkipped(eventId, now)).isTrue()

        assertThat(db.doseEventDao().getById(eventId)?.status).isEqualTo(DoseStatus.SKIPPED)
        assertThat(db.medicationDao().getById(medicationId)?.stockQuantity).isEqualTo(10.0)
    }
}
