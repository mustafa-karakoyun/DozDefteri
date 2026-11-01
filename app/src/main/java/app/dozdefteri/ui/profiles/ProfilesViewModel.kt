package app.dozdefteri.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.billing.ProStatus
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.repository.MedicationRepository
import app.dozdefteri.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class ProfilesUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val medicationCounts: Map<Long, Int> = emptyMap(),
    val activeId: Long? = null,
    val isPro: Boolean = false
) {
    val canAdd: Boolean get() = isPro || profiles.size < ProStatus.FREE_PROFILE_LIMIT
    val canDelete: Boolean get() = profiles.size > 1

    /** Colors other than the first are the Pro "profile colors". */
    fun canUseColor(index: Int): Boolean = isPro || index == 0
}

sealed interface ProfilesEvent {
    data object OpenPro : ProfilesEvent
}

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    medicationRepository: MedicationRepository,
    private val doseSync: DoseSync,
    private val notifier: DoseNotifier,
    proStatus: ProStatus,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<ProfilesUiState> = combine(
        profileRepository.profiles,
        medicationRepository.observeActiveCounts(),
        profileRepository.activeProfile,
        proStatus.isPro
    ) { profiles, counts, active, pro ->
        ProfilesUiState(profiles = profiles, medicationCounts = counts, activeId = active?.id, isPro = pro)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    private val _events = Channel<ProfilesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Returns false (and asks to open the Pro screen) when another profile needs Pro. */
    fun requestAdd(): Boolean = requirePro(uiState.value.canAdd)

    /** Returns false (and asks to open the Pro screen) when the color needs Pro. */
    fun requestColor(index: Int): Boolean = requirePro(uiState.value.canUseColor(index))

    private fun requirePro(allowed: Boolean): Boolean {
        if (!allowed) viewModelScope.launch { _events.send(ProfilesEvent.OpenPro) }
        return allowed
    }

    fun save(existing: ProfileEntity?, name: String, note: String, colorIndex: Int) {
        if (name.isBlank()) return
        val state = uiState.value
        val color = if (state.canUseColor(colorIndex)) colorIndex else 0
        viewModelScope.launch {
            if (existing == null) {
                if (state.canAdd) profileRepository.add(name, color, clock.instant(), note)
            } else {
                profileRepository.update(existing.copy(name = name, note = note, colorIndex = color))
            }
        }
    }

    fun setActive(profileId: Long) {
        viewModelScope.launch { profileRepository.setActive(profileId) }
    }

    fun delete(profile: ProfileEntity) {
        viewModelScope.launch {
            val notified = profileRepository.delete(profile) ?: return@launch
            notified.forEach(notifier::cancelDose)
            doseSync.run("profile_delete")
        }
    }

    companion object {
        const val NAME_MAX = 30
        const val NOTE_MAX = 40
    }
}
