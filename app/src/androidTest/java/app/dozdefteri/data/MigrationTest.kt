package app.dozdefteri.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.dozdefteri.data.db.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration test infrastructure. Version 1 has no migrations yet; this verifies that the exported schema
 * (app/schemas/.../1.json) exists and opens. Add `runMigrationsAndValidate` cases with every new version.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun version1_schemaCanBeCreated() {
        helper.createDatabase(TEST_DB, 1).close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
