package app.dozdefteri.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.dozdefteri.domain.model.StockChangeReason

@Entity(
    tableName = "stock_logs",
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
data class StockLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    /** Negative means a decrease. */
    val delta: Double,
    val reason: StockChangeReason,
    val createdAt: Long
)
