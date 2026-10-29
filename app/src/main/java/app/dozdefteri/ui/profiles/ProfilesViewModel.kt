package app.dozdefteri.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.billing.ProStatus
import app.dozdefteri.data.db.entity.ProfileEntity
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
    val activeId: Long? = null,
    val isPro: Boolean = false
) {
    val canAdd: Boolean get() = isPro || profiles.size < ProStatus.FREE_PROFILE_LIMIT
    val canDelete: Boolean get() = profiles.size > 1
}

sealed interface ProfilesEvent {
    data object OpenPro : ProfilesEvent
}

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val doseSync: DoseSync,
    private val notifier: DoseNotifier,
    proStatus: ProStatus,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<ProfilesUiState> = combine(
        profileRepository.profiles,
        profileRepository.activeProfile,
        proStatus.isPro
    ) { profiles, active, pro ->
        ProfilesUiState(profiles = profiles, activeId = active?.id, isPro = pro)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    private val _events = Channel<ProfilesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Returns false (and asks to open the Pro screen) when another profile needs Pro. */
    fun requestAdd(): Boolean {
        if (uiState.value.canAdd) return true
        viewModelScope.launch { _events.send(ProfilesEvent.OpenPro) }
        return false
    }

    fun add(name: String, colorIndex: Int) {
        if (!uiState.value.canAdd || name.isBlank()) return
        viewModelScope.launch { profileRepository.add(name, colorIndex, clock.instant()) }
    }

    fun update(profile: ProfileEntity, name: String, colorIndex: Int) {
        if (name.isBlank()) return
        viewModelScope.launch { profileRepository.update(profile.copy(name = name, colorIndex = colorIndex)) }
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
    }
}
