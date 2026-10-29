package app.dozdefteri.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.core.time.minuteTicker
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.prefs.SettingsRepository
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.MedicationRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.OPEN_DOSE_STATUSES
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.medications.MedicationListItem
import app.dozdefteri.ui.medications.toItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

data class TodayDose(
    val details: DoseEventDetails,
    val time: LocalTime,
    val isOverdue: Boolean,
    val canUndo: Boolean
) {
    val id: Long get() = details.event.id
    val status: DoseStatus get() = details.event.status
    val isOpen: Boolean get() = status in OPEN_DOSE_STATUSES
}

data class TodayUiState(
    val isLoading: Boolean = true,
    val profile: ProfileEntity? = null,
    val profiles: List<ProfileEntity> = emptyList(),
    val hasMedications: Boolean = false,
    /** Doses grouped by their scheduled time of day, in order. */
    val groups: List<Pair<LocalTime, List<TodayDose>>> = emptyList(),
    val asNeeded: List<MedicationListItem> = emptyList(),
    val lowStock: List<MedicationListItem> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val settingsRepository: SettingsRepository,
    private val doseSync: DoseSync,
    private val notifier: DoseNotifier,
    private val clock: Clock
) : ViewModel() {

    private val ticker = minuteTicker(clock)
    private val today = ticker.map { it.atZone(clock.zone).toLocalDate() }.distinctUntilChanged()

    val uiState: StateFlow<TodayUiState> = combine(profileRepository.activeProfile, today) { profile, date ->
        profile to date
    }.flatMapLatest { (profile, date) ->
        if (profile == null) return@flatMapLatest flowOf(TodayUiState())
        val start = date.atStartOfDay(clock.zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(clock.zone).toInstant()
        combine(
            profileRepository.profiles,
            medicationRepository.observeMedications(profile.id, archived = false),
            medicationRepository.observeSchedules(),
            doseRepository.observeRange(profile.id, start, end),
            ticker
        ) { profiles, medications, schedules, doses, now ->
            val byMedication = schedules.groupBy { it.medicationId }
            val items = medications.map { it.toItem(byMedication[it.id].orEmpty()) }
            TodayUiState(
                isLoading = false,
                profile = profile,
                profiles = profiles,
                hasMedications = medications.isNotEmpty(),
                groups = doses.map { it.toTodayDose(now) }
                    .sortedWith(compareBy({ it.time }, { it.details.medicationName.lowercase() }))
                    .groupBy { it.time }
                    .toList(),
                asNeeded = items.filter { item -> item.schedule?.type == ScheduleType.AS_NEEDED },
                lowStock = items.filter { it.isLowStock }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private fun DoseEventDetails.toTodayDose(now: Instant): TodayDose {
        val scheduled = Instant.ofEpochMilli(event.scheduledAt)
        val actionAt = event.actionAt
        return TodayDose(
            details = this,
            time = scheduled.atZone(clock.zone).toLocalTime(),
            isOverdue = event.status in OPEN_DOSE_STATUSES && scheduled.isBefore(now),
            canUndo = (event.status == DoseStatus.TAKEN || event.status == DoseStatus.SKIPPED) &&
                actionAt != null && now.toEpochMilli() - actionAt <= DoseRepository.UNDO_WINDOW.toMillis()
        )
    }

    fun take(eventId: Long) = act(eventId) { doseRepository.take(eventId, clock.instant()) }

    fun skip(eventId: Long) = act(eventId) { doseRepository.skip(eventId, clock.instant()) }

    fun snooze(eventId: Long) = act(eventId) {
        doseRepository.snooze(eventId, clock.instant(), settingsRepository.current().snoozeMinutes)
    }

    fun undo(eventId: Long) = act(eventId) { doseRepository.undo(eventId, clock.instant()) }

    fun takeAsNeeded(medicationId: Long) {
        viewModelScope.launch {
            doseRepository.takeAsNeeded(medicationId, clock.instant())
            doseSync.run("take_as_needed")
        }
    }

    fun selectProfile(profileId: Long) {
        viewModelScope.launch { profileRepository.setActive(profileId) }
    }

    private fun act(eventId: Long, block: suspend () -> Boolean) {
        viewModelScope.launch {
            block()
            notifier.cancelDose(eventId)
            doseSync.run("today")
        }
    }
}
