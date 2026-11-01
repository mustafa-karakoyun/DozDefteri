package app.dozdefteri.core.format

import android.content.Context
import android.text.format.DateFormat
import app.dozdefteri.R
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.domain.DoseGenerator
import app.dozdefteri.domain.model.ScheduleType
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Human-readable texts for schedules and enums, shared by the UI, notifications and exports. */
object ScheduleFormatter {

    /** e.g. "Every day · 08:00, 20:00", "Mon, Wed · 08:00", "Every 3 days · 09:00", "As needed". */
    fun summary(context: Context, schedule: ScheduleEntity?): String {
        if (schedule == null) return ""
        val is24Hour = DateFormat.is24HourFormat(context)
        val times = schedule.times.joinToString(", ") { Formats.time(it, is24Hour) }
        return when (schedule.type) {
            ScheduleType.DAILY -> context.getString(R.string.schedule_summary_daily, times)
            ScheduleType.WEEKDAYS -> context.getString(
                R.string.schedule_summary_weekdays,
                weekdays(schedule.daysOfWeekMask),
                times
            )
            ScheduleType.EVERY_N_DAYS -> context.resources.getQuantityString(
                R.plurals.schedule_summary_every_n_days,
                schedule.intervalDays,
                schedule.intervalDays,
                times
            )
            ScheduleType.AS_NEEDED -> context.getString(R.string.schedule_summary_as_needed)
        }
    }

    fun weekdays(mask: Int, locale: Locale = Locale.getDefault()): String =
        DayOfWeek.entries.filter { DoseGenerator.isDayInMask(mask, it) }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
}
