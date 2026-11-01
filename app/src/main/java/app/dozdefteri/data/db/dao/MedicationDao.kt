package app.dozdefteri.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.dozdefteri.data.db.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

/** Active medication count of one profile. */
data class ProfileMedicationCount(val profileId: Long, val count: Int)

@Dao
interface MedicationDao {
    @Query("SELECT profileId, COUNT(*) AS count FROM medications WHERE isArchived = 0 GROUP BY profileId")
    fun observeActiveCounts(): Flow<List<ProfileMedicationCount>>

    @Query(
        "SELECT * FROM medications WHERE profileId = :profileId AND isArchived = :archived " +
            "ORDER BY name COLLATE NOCASE"
    )
    fun observeByProfile(profileId: Long, archived: Boolean): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    fun observeById(id: Long): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE profileId = :profileId")
    suspend fun getByProfile(profileId: Long): List<MedicationEntity>

    @Query("SELECT * FROM medications ORDER BY id")
    suspend fun getAll(): List<MedicationEntity>

    @Query("SELECT COUNT(*) FROM medications")
    suspend fun count(): Int

    @Insert
    suspend fun insert(medication: MedicationEntity): Long

    @Insert
    suspend fun insertAll(medications: List<MedicationEntity>)

    @Update
    suspend fun update(medication: MedicationEntity)

    @Query("DELETE FROM medications")
    suspend fun deleteAll()
}
