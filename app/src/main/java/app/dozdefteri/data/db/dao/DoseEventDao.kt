package app.dozdefteri.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.domain.model.ScheduleType
import kotlinx.coroutines.flow.Flow

/** A dose event joined with the medication, schedule and profile fields the UI and exports need. */
data class DoseEventDetails(
    @Embedded val event: DoseEventEntity,
    val medicationName: String,
    val doseAmount: Double,
    val doseUnit: String,
    val scheduleType: ScheduleType,
    val profileName: String
)

private const val DETAILS_SELECT =
    "SELECT e.*, m.name AS medicationName, m.doseAmount AS doseAmount, m.doseUnit AS doseUnit, " +
        "s.type AS scheduleType, p.name AS profileName FROM dose_events e " +
        "INNER JOIN medications m ON m.id = e.medicationId " +
        "INNER JOIN schedules s ON s.id = e.scheduleId " +
        "INNER JOIN profiles p ON p.id = e.profileId "

private const val OPEN = "('PENDING', 'SNOOZED')"

@Dao
interface DoseEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(events: List<DoseEventEntity>): List<Long>

    @Insert
    suspend fun insert(event: DoseEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<DoseEventEntity>)

    @Update
    suspend fun update(event: DoseEventEntity)

    @Query("DELETE FROM dose_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM dose_events WHERE id = :id")
    suspend fun getById(id: Long): DoseEventEntity?

    @Query("$DETAILS_SELECT WHERE e.id = :id")
    suspend fun getDetailsById(id: Long): DoseEventDetails?

    @Query("SELECT * FROM dose_events ORDER BY id")
    suspend fun getAll(): List<DoseEventEntity>

    @Query("SELECT * FROM dose_events WHERE scheduleId = :scheduleId ORDER BY originalScheduledAt")
    suspend fun getBySchedule(scheduleId: Long): List<DoseEventEntity>

    @Query("DELETE FROM dose_events")
    suspend fun deleteAll()

    @Query("DELETE FROM dose_events WHERE scheduleId = :scheduleId AND scheduledAt > :now AND status IN $OPEN")
    suspend fun deleteFutureOpenForSchedule(scheduleId: Long, now: Long): Int

    @Query("DELETE FROM dose_events WHERE medicationId = :medicationId AND scheduledAt > :now AND status IN $OPEN")
    suspend fun deleteFutureOpenForMedication(medicationId: Long, now: Long): Int

    @Query("SELECT id FROM dose_events WHERE status IN $OPEN AND scheduledAt < :threshold")
    suspend fun getOverdueOpenIds(threshold: Long): List<Long>

    @Query("UPDATE dose_events SET status = 'MISSED', actionAt = :now WHERE status IN $OPEN AND scheduledAt < :threshold")
    suspend fun markOverdueMissed(threshold: Long, now: Long): Int

    @Query("SELECT MIN(scheduledAt) FROM dose_events WHERE status IN $OPEN AND notifiedAt IS NULL")
    suspend fun nextUnnotifiedTime(): Long?

    @Query("$DETAILS_SELECT WHERE e.status IN $OPEN AND e.notifiedAt IS NULL AND e.scheduledAt <= :limit ORDER BY e.scheduledAt")
    suspend fun getDueUnnotified(limit: Long): List<DoseEventDetails>

    @Query("UPDATE dose_events SET notifiedAt = :now WHERE id IN (:ids)")
    suspend fun markNotified(ids: List<Long>, now: Long)

    @Query("SELECT id FROM dose_events WHERE medicationId = :medicationId AND status IN $OPEN AND notifiedAt IS NOT NULL")
    suspend fun getNotifiedOpenIdsForMedication(medicationId: Long): List<Long>

    @Query("SELECT id FROM dose_events WHERE profileId = :profileId AND status IN $OPEN AND notifiedAt IS NOT NULL")
    suspend fun getNotifiedOpenIdsForProfile(profileId: Long): List<Long>

    @Query(
        "$DETAILS_SELECT WHERE (:profileId IS NULL OR e.profileId = :profileId) " +
            "AND e.originalScheduledAt >= :from AND e.originalScheduledAt < :to ORDER BY e.originalScheduledAt, e.id"
    )
    fun observeDetailsRange(profileId: Long?, from: Long, to: Long): Flow<List<DoseEventDetails>>

    @Query(
        "$DETAILS_SELECT WHERE (:profileId IS NULL OR e.profileId = :profileId) " +
            "AND e.originalScheduledAt >= :from AND e.originalScheduledAt < :to ORDER BY e.originalScheduledAt, e.id"
    )
    suspend fun getDetailsRange(profileId: Long?, from: Long, to: Long): List<DoseEventDetails>

    @Query("$DETAILS_SELECT WHERE e.profileId = :profileId AND e.status IN $OPEN ORDER BY e.scheduledAt LIMIT 1")
    suspend fun getNextOpenForProfile(profileId: Long): DoseEventDetails?
}
