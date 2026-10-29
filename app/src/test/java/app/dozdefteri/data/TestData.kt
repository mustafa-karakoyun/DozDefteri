package app.dozdefteri.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.dozdefteri.data.db.AppDatabase
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import java.time.LocalDate
import java.time.LocalTime

fun inMemoryDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder(
    ApplicationProvider.getApplicationContext<Context>(),
    AppDatabase::class.java
).allowMainThreadQueries().build()

fun testProfile(name: String = "Me") = ProfileEntity(name = name, colorIndex = 0, createdAt = 0L)

fun testMedication(
    profileId: Long,
    name: String = "Vitamin D",
    doseAmount: Double = 1.0,
    stock: Double? = null,
    threshold: Double? = null
) = MedicationEntity(
    profileId = profileId,
    name = name,
    doseAmount = doseAmount,
    doseUnit = "tablet",
    form = MedicationForm.TABLET,
    note = null,
    stockQuantity = stock,
    lowStockThreshold = threshold,
    createdAt = 0L,
    updatedAt = 0L
)

fun testSchedule(
    medicationId: Long,
    type: ScheduleType = ScheduleType.DAILY,
    times: List<String> = listOf("08:00", "20:00"),
    start: LocalDate = LocalDate.of(2026, 1, 1)
) = ScheduleEntity(
    medicationId = medicationId,
    type = type,
    times = times.map(LocalTime::parse),
    daysOfWeekMask = 0,
    intervalDays = 1,
    startDate = start,
    endDate = null
)
