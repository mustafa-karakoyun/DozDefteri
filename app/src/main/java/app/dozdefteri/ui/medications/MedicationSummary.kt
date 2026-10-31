package app.dozdefteri.ui.medications

import app.dozdefteri.core.format.Formats
import app.dozdefteri.core.format.TurkishSuffix
import app.dozdefteri.domain.model.ScheduleType
import java.time.LocalTime

/** Localized building blocks of the summary sentence, loaded from string resources by the screen. */
data class SummaryTexts(
    /** "Her gün %1$s" */
    val daily: String,
    /** "%1$s günleri %2$s" */
    val weekdays: String,
    /** "%1$d günde bir %2$s" */
    val everyNDays: String,
    /** "Gerektiğinde, hatırlatma yok" */
    val asNeeded: String,
    /** "%1$d gün boyunca" */
    val duration: String,
    /** "süresiz" */
    val indefinite: String,
    /** "ve" */
    val and: String,
    /** Adds Turkish case suffixes ("08:00'de"). */
    val turkish: Boolean
)

/**
 * Generates the live "is this right?" sentence of the medication screen, e.g.
 * "Her gün 08:00 ve 20:00'de, 1 tablet, 30 gün boyunca." Pure Kotlin.
 */
object MedicationSummary {

    fun build(
        type: ScheduleType,
        times: List<LocalTime>,
        dayNames: List<String>,
        intervalDays: Int?,
        doseText: String,
        durationDays: Int?,
        texts: SummaryTexts,
        is24Hour: Boolean = true
    ): String {
        val timeList = timeList(times.distinct().sorted(), texts, is24Hour)
        val schedulePart = when (type) {
            ScheduleType.DAILY -> texts.daily.format(timeList)
            ScheduleType.WEEKDAYS -> texts.weekdays.format(joinList(dayNames, texts.and), timeList)
            ScheduleType.EVERY_N_DAYS -> texts.everyNDays.format(intervalDays ?: 0, timeList)
            ScheduleType.AS_NEEDED -> texts.asNeeded
        }
        val durationPart = durationDays?.let { texts.duration.format(it) } ?: texts.indefinite
        return listOf(schedulePart, doseText, durationPart).joinToString(", ") + "."
    }

    /** "08:00 ve 20:00'de" (tr) / "08:00 and 20:00" (en). */
    fun timeList(times: List<LocalTime>, texts: SummaryTexts, is24Hour: Boolean = true): String {
        if (times.isEmpty()) return ""
        val formatted = times.map { Formats.time(it, is24Hour) }
        val joined = joinList(formatted, texts.and)
        return if (texts.turkish) {
            joined.removeSuffix(formatted.last()) + TurkishSuffix.timeLocative(formatted.last(), times.last())
        } else {
            joined
        }
    }

    /** "a", "a ve b", "a, b ve c". */
    fun joinList(items: List<String>, and: String): String = when (items.size) {
        0 -> ""
        1 -> items.first()
        else -> items.dropLast(1).joinToString(", ") + " $and " + items.last()
    }
}
