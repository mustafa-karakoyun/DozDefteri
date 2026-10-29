package app.dozdefteri.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.dozdefteri.data.db.AppDatabase
import app.dozdefteri.data.inMemoryDatabase
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.LowStockNotifier
import app.dozdefteri.data.testMedication
import app.dozdefteri.data.testProfile
import app.dozdefteri.data.testSchedule
import app.dozdefteri.domain.DoseGenerator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BackupManagerTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-02T05:00:00Z"), ZoneId.of("Europe/Istanbul"))
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    @Before
    fun setUp() {
        source = inMemoryDatabase()
        target = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    private suspend fun snapshot(db: AppDatabase) = BackupData(
        profiles = db.profileDao().getAll(),
        medications = db.medicationDao().getAll(),
        schedules = db.scheduleDao().getAll(),
        doseEvents = db.doseEventDao().getAll(),
        stockLogs = db.stockLogDao().getAll()
    )

    @Test
    fun exportThenImport_yieldsEqualData() = runTest {
        val profileId = source.profileDao().insert(testProfile("Ayşe"))
        val medicationId = source.medicationDao().insert(testMedication(profileId, name = "Demir, 100", stock = 10.0))
        source.scheduleDao().insert(testSchedule(medicationId))
        val repository = DoseRepository(source, DoseGenerator(), clock, LowStockNotifier { })
        repository.ensureWindow(clock.instant())
        repository.take(source.doseEventDao().getAll().first().id, clock.instant())

        val json = BackupManager(source, clock).exportJson()

        // The target already holds other data, which must be replaced entirely.
        target.profileDao().insert(testProfile("Old"))
        val manager = BackupManager(target, clock)
        manager.replaceAll(manager.parse(json))

        assertThat(snapshot(target)).isEqualTo(snapshot(source))
    }

    @Test
    fun wrongFormat_isRejected_andDataUntouched() = runTest {
        target.profileDao().insert(testProfile("Keep me"))
        val manager = BackupManager(target, clock)
        assertThrows(InvalidBackupException::class.java) {
            manager.parse("{\"format\":\"other\",\"schemaVersion\":1}")
        }
        assertThat(target.profileDao().getAll().map { it.name }).containsExactly("Keep me")
    }
}
