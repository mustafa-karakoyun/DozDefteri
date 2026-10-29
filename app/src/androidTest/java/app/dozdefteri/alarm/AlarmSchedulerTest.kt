package app.dozdefteri.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.dozdefteri.data.db.AppDatabase
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.LowStockNotifier
import app.dozdefteri.domain.DoseGenerator
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.systemDefaultZone()
    private lateinit var db: AppDatabase
    private lateinit var repository: DoseRepository
    private lateinit var scheduler: AlarmScheduler

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = DoseRepository(db, DoseGenerator(), clock, LowStockNotifier { })
        scheduler = AlarmScheduler(context, repository, clock)
    }

    @After
    fun tearDown() {
        db.close()
        existingPendingIntent()?.cancel()
    }

    private fun existingPendingIntent(): PendingIntent? = PendingIntent.getBroadcast(
        context,
        AlarmScheduler.REQUEST_CODE,
        Intent(context, DoseAlarmReceiver::class.java).setAction(AlarmScheduler.ACTION_DOSE_ALARM),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

    @Test
    fun scheduleNext_withUpcomingDose_createsPendingIntent() = runTest {
        val now = clock.millis()
        val profileId = db.profileDao().insert(ProfileEntity(name = "Me", colorIndex = 0, createdAt = now))
        val medicationId = db.medicationDao().insert(
            MedicationEntity(
                profileId = profileId,
                name = "Test",
                doseAmount = 1.0,
                doseUnit = "tablet",
                form = MedicationForm.TABLET,
                note = null,
                stockQuantity = null,
                lowStockThreshold = null,
                createdAt = now,
                updatedAt = now
            )
        )
        db.scheduleDao().insert(
            ScheduleEntity(
                medicationId = medicationId,
                type = ScheduleType.DAILY,
                times = listOf(LocalTime.of(12, 0)),
                daysOfWeekMask = 0,
                intervalDays = 1,
                startDate = clock.instant().atZone(clock.zone).toLocalDate(),
                endDate = null
            )
        )
        repository.ensureWindow(clock.instant())

        scheduler.scheduleNext()

        assertThat(existingPendingIntent()).isNotNull()
    }
}
