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
    /** Unit counted per dose and in stock, 1..20 characters, e.g. "tablet", "damla", "ml". */
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
    val updatedAt: Long,
    /** Strength per unit, e.g. 500 for "500 mg"; null when not entered. Added in schema version 2. */
    val strengthAmount: Double? = null,
    /** Unit of [strengthAmount], e.g. "mg", "mcg", "IU". */
    val strengthUnit: String? = null,
    /** Warn when the stock lasts fewer than this many days; drives [lowStockThreshold]. */
    val lowStockDays: Int? = null
)
