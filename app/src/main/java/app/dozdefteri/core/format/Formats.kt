package app.dozdefteri.core.format

import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formats {
    /** Formats a dose or stock amount with at most two decimals, e.g. 1.0 -> "1", 0.5 -> "0,5" (tr). */
    fun amount(value: Double, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getNumberInstance(locale)
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2
        return format.format(value)
    }

    /** "1 tablet" style text. */
    fun dose(amount: Double, unit: String, locale: Locale = Locale.getDefault()): String =
        "${amount(amount, locale)} $unit"

    /** Time of day as "HH:mm" (24-hour) or "h:mm a" (12-hour), following the system setting passed in. */
    fun time(time: LocalTime, is24Hour: Boolean, locale: Locale = Locale.getDefault()): String =
        time.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale))
}
