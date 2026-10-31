package app.dozdefteri.domain

import app.dozdefteri.domain.model.DoseStatus
import kotlin.math.roundToInt

/** Counts behind "Bu ay uyum: %92 — 148 planlı dozun 136'sı alındı · 5 atlandı · 7 işaretlenmedi". */
data class DoseCounts(val taken: Int = 0, val skipped: Int = 0, val missed: Int = 0, val waiting: Int = 0) {
    /** Doses with a final status; adherence = taken / planned. */
    val planned: Int get() = taken + skipped + missed
    val total: Int get() = planned + waiting

    /** Rounded adherence percentage, or null when nothing is final yet. */
    val percent: Int? get() = if (planned == 0) null else (taken * 100.0 / planned).roundToInt()
}

/** Pure Kotlin helpers for the history screen. */
object HistoryStats {
    fun count(statuses: List<DoseStatus>): DoseCounts = DoseCounts(
        taken = statuses.count { it == DoseStatus.TAKEN },
        skipped = statuses.count { it == DoseStatus.SKIPPED },
        missed = statuses.count { it == DoseStatus.MISSED },
        waiting = statuses.count { it == DoseStatus.PENDING || it == DoseStatus.SNOOZED }
    )
}
