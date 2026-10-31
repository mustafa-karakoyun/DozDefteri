package app.dozdefteri.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 0..7, index into the profile color palette. */
    val colorIndex: Int,
    /** Epoch millis. */
    val createdAt: Long,
    /** Short description, e.g. "Babam"; added in schema version 2. */
    val note: String? = null
)
