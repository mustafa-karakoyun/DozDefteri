package app.dozdefteri.export

import androidx.room.withTransaction
import app.dozdefteri.BuildConfig
import app.dozdefteri.data.db.AppDatabase
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the whole database into a JSON backup and replaces it from one. */
@Singleton
class BackupManager @Inject constructor(
    private val db: AppDatabase,
    private val clock: Clock
) {
    suspend fun exportJson(): String {
        val data = db.withTransaction {
            BackupData(
                profiles = db.profileDao().getAll(),
                medications = db.medicationDao().getAll(),
                schedules = db.scheduleDao().getAll(),
                doseEvents = db.doseEventDao().getAll(),
                stockLogs = db.stockLogDao().getAll()
            )
        }
        val exportedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS).toString()
        return BackupCodec.encode(data, BuildConfig.VERSION_CODE, exportedAt)
    }

    /** Validates [json] and returns its content, or throws [InvalidBackupException]. Nothing is written. */
    fun parse(json: String): BackupData = BackupCodec.decode(json)

    /**
     * Deletes every row and inserts the backup with its original ids, all in one transaction, so a failure leaves
     * the existing data untouched.
     */
    suspend fun replaceAll(data: BackupData) {
        db.withTransaction {
            db.stockLogDao().deleteAll()
            db.doseEventDao().deleteAll()
            db.scheduleDao().deleteAll()
            db.medicationDao().deleteAll()
            db.profileDao().deleteAll()
            db.profileDao().insertAll(data.profiles)
            db.medicationDao().insertAll(data.medications)
            db.scheduleDao().insertAll(data.schedules)
            db.doseEventDao().insertAll(data.doseEvents)
            db.stockLogDao().insertAll(data.stockLogs)
        }
    }
}
