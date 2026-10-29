package app.dozdefteri.domain

import app.dozdefteri.domain.model.DoseStatus

/** Summary state of one calendar day in the history view. */
enum class DayStatus { NONE, ALL_TAKEN, PARTIAL, MISSED }

object DayStatusCalculator {
    /**
     * [statuses] are the statuses of the day's scheduled (non AS_NEEDED) doses.
     * - no doses, or only doses still waiting (PENDING/SNOOZED) -> NONE
     * - any MISSED -> MISSED
     * - every dose TAKEN -> ALL_TAKEN
     * - anything else (skipped or a mix with waiting doses) -> PARTIAL
     */
    fun calculate(statuses: List<DoseStatus>): DayStatus {
        if (statuses.isEmpty()) return DayStatus.NONE
        if (DoseStatus.MISSED in statuses) return DayStatus.MISSED
        if (statuses.all { it == DoseStatus.TAKEN }) return DayStatus.ALL_TAKEN
        if (statuses.all { it == DoseStatus.PENDING || it == DoseStatus.SNOOZED }) return DayStatus.NONE
        return DayStatus.PARTIAL
    }
}
