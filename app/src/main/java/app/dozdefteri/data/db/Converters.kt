package app.dozdefteri.data.db

import androidx.room.TypeConverter
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.domain.model.StockChangeReason
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Room converters. Enums are stored by `name`, dates as ISO strings, times as "HH:mm" joined by commas. */
class Converters {
    @TypeConverter
    fun timesToString(times: List<LocalTime>): String =
        times.distinct().sorted().joinToString(",") { it.format(TIME_FORMAT) }

    @TypeConverter
    fun stringToTimes(value: String): List<LocalTime> =
        if (value.isBlank()) emptyList() else value.split(",").map { LocalTime.parse(it.trim(), TIME_FORMAT) }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun scheduleTypeToString(value: ScheduleType): String = value.name

    @TypeConverter
    fun stringToScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)

    @TypeConverter
    fun doseStatusToString(value: DoseStatus): String = value.name

    @TypeConverter
    fun stringToDoseStatus(value: String): DoseStatus = DoseStatus.valueOf(value)

    @TypeConverter
    fun formToString(value: MedicationForm): String = value.name

    @TypeConverter
    fun stringToForm(value: String): MedicationForm = MedicationForm.valueOf(value)

    @TypeConverter
    fun reasonToString(value: StockChangeReason): String = value.name

    @TypeConverter
    fun stringToReason(value: String): StockChangeReason = StockChangeReason.valueOf(value)

    companion object {
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
