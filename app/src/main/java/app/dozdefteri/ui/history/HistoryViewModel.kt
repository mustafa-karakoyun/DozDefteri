package app.dozdefteri.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.DayStatus
import app.dozdefteri.domain.DayStatusCalculator
import app.dozdefteri.domain.DoseCounts
import app.dozdefteri.domain.HistoryStats
import app.dozdefteri.domain.model.ScheduleType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val month: YearMonth,
    val today: LocalDate,
    val summary: DoseCounts = DoseCounts(),
    val dayStatuses: Map<LocalDate, DayStatus> = emptyMap(),
    val dayCounts: Map<LocalDate, DoseCounts> = emptyMap(),
    val dosesByDay: Map<LocalDate, List<DoseEventDetails>> = emptyMap(),
    val selectedDay: LocalDate? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val doseRepository: DoseRepository,
    private val doseSync: DoseSync,
    private val notifier: DoseNotifier,
    private val clock: Clock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val selectedDay = MutableStateFlow<LocalDate?>(LocalDate.now(clock))

    val uiState: StateFlow<HistoryUiState> = combine(profileRepository.activeProfile, month) { profile, month ->
        profile to month
    }.flatMapLatest { (profile, month) ->
        if (profile == null) return@flatMapLatest flowOf(HistoryUiState(month = month, today = today()))
        val start = month.atDay(1).atStartOfDay(clock.zone).toInstant()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toInstant()
        doseRepository.observeRange(profile.id, start, end).map { doses -> build(month, doses) }
    }.combine(selectedDay) { state, day ->
        state.copy(selectedDay = day?.takeIf { YearMonth.from(it) == state.month })
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HistoryUiState(month = YearMonth.now(clock), today = today())
    )

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun build(month: YearMonth, doses: List<DoseEventDetails>): HistoryUiState {
        val byDay = doses.groupBy { dayOf(it) }
        val scheduledByDay = byDay.mapValues { (_, list) ->
            list.filter { it.scheduleType != ScheduleType.AS_NEEDED }.map { it.event.status }
        }
        return HistoryUiState(
            isLoading = false,
            month = month,
            today = today(),
            summary = HistoryStats.count(scheduledByDay.values.flatten()),
            dayStatuses = scheduledByDay.mapValues { (_, statuses) -> DayStatusCalculator.calculate(statuses) },
            dayCounts = scheduledByDay.mapValues { (_, statuses) -> HistoryStats.count(statuses) },
            dosesByDay = byDay
        )
    }

    private fun dayOf(details: DoseEventDetails): LocalDate =
        Instant.ofEpochMilli(details.event.originalScheduledAt).atZone(clock.zone).toLocalDate()

    fun previousMonth() = changeMonth { it.minusMonths(1) }

    fun nextMonth() = changeMonth { it.plusMonths(1) }

    private fun changeMonth(transform: (YearMonth) -> YearMonth) {
        month.update(transform)
        val newMonth = month.value
        selectedDay.value = if (newMonth == YearMonth.now(clock)) today() else null
    }

    fun selectDay(day: LocalDate) {
        selectedDay.value = day
    }

    /** Corrects a past dose: taken at [time] on its planned day. */
    fun markTakenAt(details: DoseEventDetails, time: LocalTime) {
        viewModelScope.launch {
            val takenAt = dayOf(details).atTime(time).atZone(clock.zone).toInstant()
            doseRepository.markTakenAt(details.event.id, takenAt, clock.instant())
            after(details.event.id)
        }
    }

    fun markSkipped(eventId: Long) {
        viewModelScope.launch {
            doseRepository.markSkipped(eventId, clock.instant())
            after(eventId)
        }
    }

    private suspend fun after(eventId: Long) {
        notifier.cancelDose(eventId)
        doseSync.run("history_edit")
    }
}
