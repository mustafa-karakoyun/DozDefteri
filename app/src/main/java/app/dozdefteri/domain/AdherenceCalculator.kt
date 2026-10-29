package app.dozdefteri.domain

import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.domain.model.DoseStatus
import javax.inject.Inject
import kotlin.math.roundToInt

/** Adherence percentage calculation. Pure Kotlin. */
class AdherenceCalculator @Inject constructor() {

    /**
     * Returns `TAKEN / (TAKEN + SKIPPED + MISSED) * 100` rounded to an integer, or `null` when the denominator is 0.
     * Events whose schedule id is in [asNeededScheduleIds] are ignored; PENDING and SNOOZED events never count.
     */
    fun calculate(events: List<DoseEventEntity>, asNeededScheduleIds: Set<Long> = emptySet()): Int? {
        var taken = 0
        var total = 0
        for (event in events) {
            if (event.scheduleId in asNeededScheduleIds) continue
            when (event.status) {
                DoseStatus.TAKEN -> {
                    taken++
                    total++
                }
                DoseStatus.SKIPPED, DoseStatus.MISSED -> total++
                DoseStatus.PENDING, DoseStatus.SNOOZED -> Unit
            }
        }
        if (total == 0) return null
        return (taken * 100.0 / total).roundToInt()
    }
}
