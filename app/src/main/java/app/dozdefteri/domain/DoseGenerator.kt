package app.dozdefteri.domain

import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.domain.model.ScheduleType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Expands a [ScheduleEntity] into concrete dose instants. Pure Kotlin; no Android dependencies. */
class DoseGenerator @Inject constructor() {

    /**
     * Returns the dose instants the schedule produces in `[from, to)`, sorted ascending and without duplicates.
     *
     * Every local date between the local dates of [from] and [to] (both inclusive) that lies within the
     * schedule's `startDate..endDate` and matches its type is combined with each of the schedule's times via
     * [ZonedDateTime.of]. That keeps Java's default DST handling: a time inside a spring-forward gap moves forward
     * by the gap length and an ambiguous fall-back time resolves to the earlier offset, so each dose occurs once.
     * [ScheduleType.AS_NEEDED] schedules never produce doses.
     */
    fun generate(schedule: ScheduleEntity, from: Instant, to: Instant, zone: ZoneId): List<Instant> {
        if (schedule.type == ScheduleType.AS_NEEDED || schedule.times.isEmpty() || !from.isBefore(to)) {
            return emptyList()
        }
        val firstDate = maxOf(from.atZone(zone).toLocalDate(), schedule.startDate)
        val lastDate = to.atZone(zone).toLocalDate().let { end ->
            schedule.endDate?.let { minOf(end, it) } ?: end
        }
        val result = sortedSetOf<Instant>()
        var date = firstDate
        while (!date.isAfter(lastDate)) {
            if (matches(schedule, date)) {
                for (time in schedule.times) {
                    val instant = ZonedDateTime.of(date, time, zone).toInstant()
                    if (!instant.isBefore(from) && instant.isBefore(to)) {
                        result.add(instant)
                    }
                }
            }
            date = date.plusDays(1)
        }
        return result.toList()
    }

    private fun matches(schedule: ScheduleEntity, date: LocalDate): Boolean = when (schedule.type) {
        ScheduleType.DAILY -> true
        ScheduleType.WEEKDAYS -> isDayInMask(schedule.daysOfWeekMask, date.dayOfWeek)
        ScheduleType.EVERY_N_DAYS -> {
            val interval = schedule.intervalDays.coerceAtLeast(1)
            ChronoUnit.DAYS.between(schedule.startDate, date) % interval == 0L
        }
        ScheduleType.AS_NEEDED -> false
    }

    companion object {
        /** Bit for [day] in a days-of-week mask (Monday = bit 0 ... Sunday = bit 6). */
        fun dayBit(day: DayOfWeek): Int = 1 shl (day.value - 1)

        fun isDayInMask(mask: Int, day: DayOfWeek): Boolean = mask and dayBit(day) != 0

        fun maskOf(days: Collection<DayOfWeek>): Int = days.fold(0) { acc, day -> acc or dayBit(day) }
    }
}
