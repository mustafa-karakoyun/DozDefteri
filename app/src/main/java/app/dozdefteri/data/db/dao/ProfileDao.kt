package app.dozdefteri.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.dozdefteri.data.db.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt, id")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY createdAt, id")
    suspend fun getAll(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Insert
    suspend fun insertAll(profiles: List<ProfileEntity>)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}
