package app.dozdefteri.ui.today

import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.OPEN_DOSE_STATUSES
import java.time.Instant
import java.time.LocalTime

/** Part of the day a dose belongs to, by its planned time (plan screen 2). */
enum class DayPart {
    MORNING,
    NOON,
    EVENING;

    companion object {
        fun of(time: LocalTime): DayPart = when {
            time.hour < 12 -> MORNING
            time.hour < 17 -> NOON
            else -> EVENING
        }
    }
}

data class TodayDose(
    val details: DoseEventDetails,
    val plannedTime: LocalTime,
    val scheduledTime: LocalTime,
    val takenTime: LocalTime?,
    val canUndo: Boolean
) {
    val id: Long get() = details.event.id
    val status: DoseStatus get() = details.event.status
    val isOpen: Boolean get() = status in OPEN_DOSE_STATUSES
}

data class LowStockItem(val medication: MedicationEntity, val remainingDays: Int?, val fraction: Float)

data class TodayGroup(val part: DayPart, val doses: List<TodayDose>, val lowStock: List<LowStockItem>)

data class PrnItem(val medication: MedicationEntity, val lastTakenAt: Instant?)

data class NextDose(val name: String, val time: LocalTime, val snoozed: Boolean)

/** Groups today's scheduled doses into morning/noon/evening and places each low-stock card after its last dose. */
object TodayGrouping {
    fun build(doses: List<TodayDose>, lowStock: List<LowStockItem>): List<TodayGroup> {
        val byPart = doses.sortedWith(compareBy({ it.plannedTime }, { it.details.medicationName.lowercase() }))
            .groupBy { DayPart.of(it.plannedTime) }
        val parts = DayPart.entries.filter { byPart[it].orEmpty().isNotEmpty() }
        val lowStockByPart = lowStock.groupBy { item ->
            parts.lastOrNull { part ->
                byPart[part].orEmpty().any { it.details.event.medicationId == item.medication.id }
            } ?: parts.lastOrNull()
        }
        return parts.map { part -> TodayGroup(part, byPart[part].orEmpty(), lowStockByPart[part].orEmpty()) }
    }

    /** Earliest open dose by its current time; snoozed doses use the new time. */
    fun next(doses: List<TodayDose>): NextDose? = doses.filter { it.isOpen }
        .minByOrNull { it.scheduledTime }
        ?.let { NextDose(it.details.medicationName, it.scheduledTime, it.status == DoseStatus.SNOOZED) }

    /** Low-stock cards whose medication has no dose today (shown before the as-needed section). */
    fun unplaced(groups: List<TodayGroup>, lowStock: List<LowStockItem>): List<LowStockItem> =
        if (groups.isEmpty()) lowStock else emptyList()
}
