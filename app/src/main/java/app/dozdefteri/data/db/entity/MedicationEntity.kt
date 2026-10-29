package app.dozdefteri.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.dozdefteri.domain.model.MedicationForm

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    /** 1..60 characters, trimmed. */
    val name: String,
    /** Greater than 0, e.g. 1.0 or 0.5. */
    val doseAmount: Double,
    /** Free text, 1..20 characters, e.g. "tablet", "mg", "ml". */
    val doseUnit: String,
    val form: MedicationForm,
    /** At most 200 characters. */
    val note: String?,
    /** null means stock tracking is off. */
    val stockQuantity: Double?,
    /** null whenever [stockQuantity] is null. */
    val lowStockThreshold: Double?,
    val lowStockNotified: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
