package app.dozdefteri.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.dozdefteri.data.db.AppDatabase
import app.dozdefteri.data.db.MIGRATION_1_2
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies every schema migration against the exported schemas in app/schemas. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2_keepsExistingRows() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO profiles (id, name, colorIndex, createdAt) VALUES (1, 'Ayşe', 0, 0)")
            execSQL(
                "INSERT INTO medications (id, profileId, name, doseAmount, doseUnit, form, note, stockQuantity, " +
                    "lowStockThreshold, lowStockNotified, isArchived, createdAt, updatedAt) " +
                    "VALUES (1, 1, 'Metformin', 1.0, 'tablet', 'TABLET', NULL, 38.0, 8.0, 0, 0, 0, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT name, stockQuantity, strengthAmount, lowStockDays FROM medications").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Metformin")
            assertThat(cursor.getDouble(1)).isEqualTo(38.0)
            assertThat(cursor.isNull(2)).isTrue()
            assertThat(cursor.isNull(3)).isTrue()
        }
        db.query("SELECT note FROM profiles").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.isNull(0)).isTrue()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
