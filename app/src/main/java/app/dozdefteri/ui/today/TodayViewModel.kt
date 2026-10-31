package app.dozdefteri.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.core.time.minuteTicker
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.MedicationRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.StockEstimator
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.ScheduleType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class TodayUiState(
    val isLoading: Boolean = true,
    val today: LocalDate? = null,
    val hasMedications: Boolean = false,
    val groups: List<TodayGroup> = emptyList(),
    /** Statuses of today's scheduled doses in time order (progress segments). */
    val segments: List<DoseStatus> = emptyList(),
    val next: NextDose? = null,
    val allDone: Boolean = false,
    val unplacedLowStock: List<LowStockItem> = emptyList(),
    val prn: List<PrnItem> = emptyList()
)

sealed interface TodayEvent {
    data class Taken(val eventId: Long, val medicationName: String) : TodayEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val doseSync: DoseSync,
    private val notifier: DoseNotifier,
    private val clock: Clock
) : ViewModel() {

    private val ticker = minuteTicker(clock)
    private val today = ticker.map { it.atZone(clock.zone).toLocalDate() }.distinctUntilChanged()

    private val _events = Channel<TodayEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<TodayUiState> = combine(profileRepository.activeProfile, today) { profile, date ->
        profile to date
    }.flatMapLatest { (profile, date) ->
        if (profile == null) return@flatMapLatest flowOf(TodayUiState())
        val start = date.atStartOfDay(clock.zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(clock.zone).toInstant()
        combine(
            medicationRepository.observeMedications(profile.id, archived = false),
            medicationRepository.observeSchedules(),
            doseRepository.observeRange(profile.id, start, end),
            ticker
        ) { medications, schedules, events, now ->
            val byMedication = schedules.groupBy { it.medicationId }
            val scheduled = events.filter { it.scheduleType != ScheduleType.AS_NEEDED }.map { it.toTodayDose(now) }
            val lowStock = medications.mapNotNull { medication ->
                val stock = medication.stockQuantity ?: return@mapNotNull null
                val threshold = medication.lowStockThreshold ?: return@mapNotNull null
                if (stock > threshold) return@mapNotNull null
                val days = StockEstimator.remainingDays(
                    stock,
                    byMedication[medication.id].orEmpty(),
                    medication.doseAmount
                )
                LowStockItem(medication, days, stockFraction(days, stock))
            }
            val groups = TodayGrouping.build(scheduled, lowStock)
            val prn = medications
                .filter { med -> byMedication[med.id].orEmpty().any { it.type == ScheduleType.AS_NEEDED } }
                .map { PrnItem(it, doseRepository.lastTakenAt(it.id)) }
            TodayUiState(
                isLoading = false,
                today = date,
                hasMedications = medications.isNotEmpty(),
                groups = groups,
                segments = scheduled.sortedBy { it.plannedTime }.map { it.status },
                next = TodayGrouping.next(scheduled),
                allDone = scheduled.isNotEmpty() && scheduled.none { it.isOpen },
                unplacedLowStock = TodayGrouping.unplaced(groups, lowStock),
                prn = prn
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private fun DoseEventDetails.toTodayDose(now: Instant): TodayDose {
        val actionAt = event.actionAt
        return TodayDose(
            details = this,
            plannedTime = Instant.ofEpochMilli(event.originalScheduledAt).atZone(clock.zone).toLocalTime(),
            scheduledTime = Instant.ofEpochMilli(event.scheduledAt).atZone(clock.zone).toLocalTime(),
            takenTime = if (event.status == DoseStatus.TAKEN && actionAt != null) {
                Instant.ofEpochMilli(actionAt).atZone(clock.zone).toLocalTime()
            } else {
                null
            },
            canUndo = (event.status == DoseStatus.TAKEN || event.status == DoseStatus.SKIPPED) &&
                actionAt != null && now.toEpochMilli() - actionAt <= DoseRepository.UNDO_WINDOW.toMillis()
        )
    }

    fun take(dose: TodayDose) {
        viewModelScope.launch {
            if (doseRepository.take(dose.id, clock.instant())) {
                _events.send(TodayEvent.Taken(dose.id, dose.details.medicationName))
            }
            afterChange(dose.id)
        }
    }

    fun undo(eventId: Long) = act(eventId) { doseRepository.undo(eventId, clock.instant()) }

    fun markSkipped(eventId: Long) = act(eventId) { doseRepository.markSkipped(eventId, clock.instant()) }

    /** Records a dose as taken at [time] on the day it was planned for. */
    fun markTakenAt(dose: TodayDose, time: LocalTime) = act(dose.id) {
        val day = Instant.ofEpochMilli(dose.details.event.originalScheduledAt).atZone(clock.zone).toLocalDate()
        doseRepository.markTakenAt(dose.id, day.atTime(time).atZone(clock.zone).toInstant(), clock.instant())
    }

    fun takeAsNeeded(medicationId: Long) {
        viewModelScope.launch {
            doseRepository.takeAsNeeded(medicationId, clock.instant())
            doseSync.run("take_as_needed")
        }
    }

    fun addStock(medicationId: Long, amount: Double) {
        viewModelScope.launch { medicationRepository.addStock(medicationId, amount, clock.instant()) }
    }

    private fun act(eventId: Long, block: suspend () -> Boolean) {
        viewModelScope.launch {
            block()
            afterChange(eventId)
        }
    }

    private suspend fun afterChange(eventId: Long) {
        notifier.cancelDose(eventId)
        doseSync.run("today")
    }

    companion object {
        /** A full stock bar means a month of doses. */
        const val FULL_STOCK_DAYS = 30f

        fun stockFraction(days: Int?, stock: Double): Float =
            ((days?.toFloat() ?: stock.toFloat()) / FULL_STOCK_DAYS).coerceIn(0f, 1f)
    }
}
