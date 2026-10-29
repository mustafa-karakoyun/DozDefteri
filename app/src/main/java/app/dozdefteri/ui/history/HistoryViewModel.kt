package app.dozdefteri.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.AdherenceCalculator
import app.dozdefteri.domain.DayStatus
import app.dozdefteri.domain.DayStatusCalculator
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val month: YearMonth,
    val today: LocalDate,
    /** null when no dose of the month has a final status yet. */
    val adherencePercent: Int? = null,
    val dayStatuses: Map<LocalDate, DayStatus> = emptyMap(),
    val dosesByDay: Map<LocalDate, List<DoseEventDetails>> = emptyMap(),
    val selectedDay: LocalDate? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val doseRepository: DoseRepository,
    private val adherenceCalculator: AdherenceCalculator,
    private val clock: Clock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(profileRepository.activeProfile, month) { profile, month ->
        profile to month
    }.flatMapLatest { (profile, month) ->
        if (profile == null) return@flatMapLatest flowOf(HistoryUiState(month = month, today = today()))
        val start = month.atDay(1).atStartOfDay(clock.zone).toInstant()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toInstant()
        doseRepository.observeRange(profile.id, start, end).map { doses -> build(month, doses) }
    }.combine(selectedDay) { state, day -> state.copy(selectedDay = day) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HistoryUiState(month = YearMonth.now(clock), today = today())
        )

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun build(month: YearMonth, doses: List<DoseEventDetails>): HistoryUiState {
        val byDay = doses.groupBy {
            Instant.ofEpochMilli(it.event.originalScheduledAt).atZone(clock.zone).toLocalDate()
        }
        val scheduled = doses.filter { it.scheduleType != ScheduleType.AS_NEEDED }
        return HistoryUiState(
            isLoading = false,
            month = month,
            today = today(),
            adherencePercent = adherenceCalculator.calculate(scheduled.map { it.event }),
            dayStatuses = byDay.mapValues { (_, list) ->
                DayStatusCalculator.calculate(
                    list.filter { it.scheduleType != ScheduleType.AS_NEEDED }.map { it.event.status }
                )
            },
            dosesByDay = byDay
        )
    }

    fun previousMonth() {
        month.update { it.minusMonths(1) }
        selectedDay.value = null
    }

    fun nextMonth() {
        month.update { it.plusMonths(1) }
        selectedDay.value = null
    }

    fun selectDay(day: LocalDate?) {
        selectedDay.value = day
    }
}
