package app.dozdefteri.export

import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.data.db.entity.StockLogEntity
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.domain.model.StockChangeReason
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class BackupCodecTest {

    private val data = BackupData(
        profiles = listOf(ProfileEntity(id = 3, name = "Ayşe", colorIndex = 2, createdAt = 10, note = "Annem")),
        medications = listOf(
            MedicationEntity(
                id = 7,
                profileId = 3,
                name = "Demir, \"100\"",
                doseAmount = 0.5,
                doseUnit = "mg",
                form = MedicationForm.DROPS,
                note = "Aç karnına",
                stockQuantity = 12.25,
                lowStockThreshold = 3.0,
                lowStockNotified = true,
                isArchived = false,
                createdAt = 11,
                updatedAt = 12,
                strengthAmount = 100.0,
                strengthUnit = "mg",
                lowStockDays = 7
            )
        ),
        schedules = listOf(
            ScheduleEntity(
                id = 9,
                medicationId = 7,
                type = ScheduleType.WEEKDAYS,
                times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 30)),
                daysOfWeekMask = 0b0010101,
                intervalDays = 1,
                startDate = LocalDate.of(2026, 11, 2),
                endDate = LocalDate.of(2027, 1, 1)
            )
        ),
        doseEvents = listOf(
            DoseEventEntity(
                id = 100,
                scheduleId = 9,
                medicationId = 7,
                profileId = 3,
                originalScheduledAt = 1000,
                scheduledAt = 2000,
                status = DoseStatus.SNOOZED,
                notifiedAt = null,
                actionAt = null,
                quantity = 0.5
            )
        ),
        stockLogs = listOf(
            StockLogEntity(
                id = 5,
                medicationId = 7,
                delta = -0.5,
                reason = StockChangeReason.DOSE_TAKEN,
                createdAt = 13
            )
        )
    )

    @Test
    fun roundTrip_preservesEverything() {
        val json = BackupCodec.encode(data, appVersionCode = 1, exportedAt = "2026-11-20T10:15:00Z")
        assertThat(json).contains("\"format\": \"dozdefteri-backup\"")
        assertThat(json).contains("\"schemaVersion\": 2")
        assertThat(BackupCodec.decode(json)).isEqualTo(data)
    }

    @Test
    fun wrongFormat_isRejected() {
        val json = BackupCodec.encode(data, 1, "x").replace("dozdefteri-backup", "something-else")
        val error = assertThrows(InvalidBackupException::class.java) { BackupCodec.decode(json) }
        assertThat(error.reason).isEqualTo(InvalidBackupException.Reason.WRONG_FORMAT)
    }

    @Test
    fun newerSchema_isRejected() {
        val json = BackupCodec.encode(data, 1, "x").replace("\"schemaVersion\": 2", "\"schemaVersion\": 3")
        val error = assertThrows(InvalidBackupException::class.java) { BackupCodec.decode(json) }
        assertThat(error.reason).isEqualTo(InvalidBackupException.Reason.UNSUPPORTED_VERSION)
    }

    @Test
    fun version1Backup_isAccepted_withDefaultsForNewFields() {
        val v1 = data.copy(
            profiles = data.profiles.map { it.copy(note = null) },
            medications = data.medications.map {
                it.copy(strengthAmount = null, strengthUnit = null, lowStockDays = null)
            }
        )
        val json = BackupCodec.encode(v1, 1, "x")
            .replace("\"schemaVersion\": 2", "\"schemaVersion\": 1")
            .replace(Regex(",\\s*\"(note|strengthAmount|strengthUnit|lowStockDays)\": null"), "")
        assertThat(json).doesNotContain("strengthUnit")
        assertThat(BackupCodec.decode(json)).isEqualTo(v1)
    }

    @Test
    fun garbageAndOtherJson_areRejected() {
        assertThat(assertThrows(InvalidBackupException::class.java) { BackupCodec.decode("not json") }.reason)
            .isEqualTo(InvalidBackupException.Reason.NOT_JSON)
        assertThat(assertThrows(InvalidBackupException::class.java) { BackupCodec.decode("{\"a\":1}") }.reason)
            .isEqualTo(InvalidBackupException.Reason.WRONG_FORMAT)
    }

    @Test
    fun danglingReference_isRejected() {
        val broken = data.copy(schedules = data.schedules.map { it.copy(medicationId = 999) })
        val json = BackupCodec.encode(broken, 1, "x")
        assertThat(assertThrows(InvalidBackupException::class.java) { BackupCodec.decode(json) }.reason)
            .isEqualTo(InvalidBackupException.Reason.INVALID_CONTENT)
    }
}
