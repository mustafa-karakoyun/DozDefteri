package app.dozdefteri.core.debug

import androidx.room.withTransaction
import app.dozdefteri.data.db.AppDatabase
import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/** Debug-only helper: creates a test dose one minute from now to verify notifications end to end. */
class DebugDoseCreator @Inject constructor(
    private val db: AppDatabase,
    private val profileRepository: ProfileRepository,
    private val clock: Clock
) {
    suspend fun createTestDose(medicationName: String): Boolean {
        val profile = profileRepository.getActiveProfile() ?: return false
        val now = clock.instant()
        val millis = now.toEpochMilli()
        val doseAt = now.plus(Duration.ofMinutes(1)).toEpochMilli()
        db.withTransaction {
            // AS_NEEDED keeps the generator from creating more doses and excludes the test from adherence.
            val medicationId = db.medicationDao().insert(
                MedicationEntity(
                    profileId = profile.id,
                    name = medicationName,
                    doseAmount = 1.0,
                    doseUnit = "tablet",
                    form = MedicationForm.TABLET,
                    note = null,
                    stockQuantity = null,
                    lowStockThreshold = null,
                    createdAt = millis,
                    updatedAt = millis
                )
            )
            val scheduleId = db.scheduleDao().insert(
                ScheduleEntity(
                    medicationId = medicationId,
                    type = ScheduleType.AS_NEEDED,
                    times = emptyList(),
                    daysOfWeekMask = 0,
                    intervalDays = 1,
                    startDate = now.atZone(clock.zone).toLocalDate(),
                    endDate = null
                )
            )
            db.doseEventDao().insert(
                DoseEventEntity(
                    scheduleId = scheduleId,
                    medicationId = medicationId,
                    profileId = profile.id,
                    originalScheduledAt = doseAt,
                    scheduledAt = doseAt,
                    status = DoseStatus.PENDING,
                    notifiedAt = null,
                    actionAt = null,
                    quantity = 1.0
                )
            )
        }
        return true
    }
}
