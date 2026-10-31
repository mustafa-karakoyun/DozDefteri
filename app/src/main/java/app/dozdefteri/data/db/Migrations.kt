package app.dozdefteri.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: strength and low-stock-days on medications, a short note on profiles. Existing rows keep their data. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE medications ADD COLUMN strengthAmount REAL")
        db.execSQL("ALTER TABLE medications ADD COLUMN strengthUnit TEXT")
        db.execSQL("ALTER TABLE medications ADD COLUMN lowStockDays INTEGER")
        db.execSQL("ALTER TABLE profiles ADD COLUMN note TEXT")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
