package app.dozdefteri.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.dozdefteri.domain.model.DoseStatus

@Entity(
    tableName = "dose_events",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["scheduleId", "originalScheduledAt"], unique = true),
        Index("scheduledAt"),
        Index("status")
    ]
)
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long,
    /** Denormalized for queries. */
    val medicationId: Long,
    /** Denormalized for queries. */
    val profileId: Long,
    /** Time produced by the schedule; never changes and is the de-duplication key. */
    val originalScheduledAt: Long,
    /** Changes when snoozed. */
    val scheduledAt: Long,
    val status: DoseStatus,
    /** When the notification was shown; prevents repeated notifications. */
    val notifiedAt: Long?,
    /** When the dose became TAKEN, SKIPPED or MISSED. */
    val actionAt: Long?,
    /** Amount for this dose, copied from the medication's dose amount. */
    val quantity: Double
)
