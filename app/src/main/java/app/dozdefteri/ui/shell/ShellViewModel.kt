package app.dozdefteri.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.model.OPEN_DOSE_STATUSES
import app.dozdefteri.domain.model.ScheduleType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

/** One row of the profile switcher: who, and what they need next today. */
data class ProfileSummary(
    val profile: ProfileEntity,
    /** Time of the next open dose today; null when none is left. */
    val nextTime: LocalTime?,
    /** Number of open doses at [nextTime]. */
    val nextCount: Int,
    /** True when the profile had doses today and all of them are done. */
    val allDone: Boolean
)

/** App-shell state: the active profile shown in every header, and the profile switcher sheet. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val doseRepository: DoseRepository,
    private val clock: Clock
) : ViewModel() {

    val activeProfile: StateFlow<ProfileEntity?> =
        profileRepository.activeProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _summaries = MutableStateFlow<List<ProfileSummary>>(emptyList())
    val summaries: StateFlow<List<ProfileSummary>> = _summaries.asStateFlow()

    /** Loads the switcher rows; called when the sheet opens. */
    fun loadSummaries() {
        viewModelScope.launch {
            val today = clock.instant().atZone(clock.zone).toLocalDate()
            val start = today.atStartOfDay(clock.zone).toInstant()
            val end = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
            _summaries.value = profileRepository.getAll().map { profile ->
                val doses = doseRepository.getRange(profile.id, start, end)
                    .filter { it.scheduleType != ScheduleType.AS_NEEDED }
                val open = doses.filter { it.event.status in OPEN_DOSE_STATUSES }
                val nextMinute = open.minOfOrNull { it.event.scheduledAt / MINUTE }
                ProfileSummary(
                    profile = profile,
                    nextTime = nextMinute?.let { Instant.ofEpochMilli(it * MINUTE).atZone(clock.zone).toLocalTime() },
                    nextCount = open.count { it.event.scheduledAt / MINUTE == nextMinute },
                    allDone = doses.isNotEmpty() && open.isEmpty()
                )
            }
        }
    }

    fun switchTo(profileId: Long) {
        viewModelScope.launch { profileRepository.setActive(profileId) }
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
