package app.dozdefteri.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.dozdefteri.data.db.dao.DoseEventDao
import app.dozdefteri.data.db.dao.MedicationDao
import app.dozdefteri.data.db.dao.ProfileDao
import app.dozdefteri.data.db.dao.ScheduleDao
import app.dozdefteri.data.db.dao.StockLogDao
import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.data.db.entity.StockLogEntity

@Database(
    entities = [
        ProfileEntity::class,
        MedicationEntity::class,
        ScheduleEntity::class,
        DoseEventEntity::class,
        StockLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseEventDao(): DoseEventDao
    abstract fun stockLogDao(): StockLogDao

    companion object {
        const val NAME = "dozdefteri.db"
    }
}
