package app.dozdefteri.domain

import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.domain.model.ScheduleType
import kotlin.math.floor

/** Estimates how long the remaining stock lasts. Pure Kotlin. */
object StockEstimator {

    /**
     * Average amount used per day by the given schedules. AS_NEEDED schedules are excluded.
     * WEEKDAYS spreads the selected days over a week; EVERY_N_DAYS divides by the interval.
     */
    fun dailyAverage(schedules: List<ScheduleEntity>, doseAmount: Double): Double =
        schedules.sumOf { schedule ->
            val perDay = schedule.times.size * doseAmount
            when (schedule.type) {
                ScheduleType.DAILY -> perDay
                ScheduleType.WEEKDAYS -> perDay * Integer.bitCount(schedule.daysOfWeekMask and 0x7F) / 7.0
                ScheduleType.EVERY_N_DAYS -> perDay / schedule.intervalDays.coerceAtLeast(1)
                ScheduleType.AS_NEEDED -> 0.0
            }
        }

    /** Whole days the [stock] lasts, or `null` when no estimate is possible. */
    fun remainingDays(stock: Double?, schedules: List<ScheduleEntity>, doseAmount: Double): Int? {
        if (stock == null) return null
        val average = dailyAverage(schedules, doseAmount)
        if (average <= 0.0) return null
        return floor(stock / average).toInt()
    }
}
