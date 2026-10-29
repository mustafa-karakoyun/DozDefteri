package app.dozdefteri.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.dozdefteri.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

/** A schedule of a non-archived medication together with the fields needed to create dose events. */
data class ActiveSchedule(
    @Embedded val schedule: ScheduleEntity,
    val profileId: Long,
    val doseAmount: Double
)

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId ORDER BY id")
    suspend fun getByMedication(medicationId: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedules ORDER BY id")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntity?

    @Query("SELECT * FROM schedules ORDER BY id")
    suspend fun getAll(): List<ScheduleEntity>

    @Query(
        "SELECT s.*, m.profileId AS profileId, m.doseAmount AS doseAmount FROM schedules s " +
            "INNER JOIN medications m ON m.id = s.medicationId WHERE m.isArchived = 0"
    )
    suspend fun getActive(): List<ActiveSchedule>

    @Insert
    suspend fun insert(schedule: ScheduleEntity): Long

    @Insert
    suspend fun insertAll(schedules: List<ScheduleEntity>)

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules")
    suspend fun deleteAll()
}
