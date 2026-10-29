package app.dozdefteri.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.dozdefteri.domain.model.ScheduleType
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId")]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val type: ScheduleType,
    /** Sorted, distinct. Empty for [ScheduleType.AS_NEEDED]. */
    val times: List<LocalTime>,
    /** WEEKDAYS bit mask: Monday = 1 shl 0 ... Sunday = 1 shl 6. 0 for other types. */
    val daysOfWeekMask: Int,
    /** EVERY_N_DAYS interval, 2..30. 1 for other types. */
    val intervalDays: Int,
    val startDate: LocalDate,
    /** Inclusive; null means open-ended. */
    val endDate: LocalDate?
)
