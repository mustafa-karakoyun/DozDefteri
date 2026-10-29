package app.dozdefteri.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.billing.ProStatus
import app.dozdefteri.data.prefs.AppSettings
import app.dozdefteri.data.prefs.SettingsRepository
import app.dozdefteri.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isPro: Boolean = false
) {
    /** The accent actually applied: Pro accents fall back to the free one without Pro. */
    val effectiveAccent: Int get() = if (isPro) settings.accentIndex else 0
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val doseSync: DoseSync,
    proStatus: ProStatus
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(settingsRepository.settings, proStatus.isPro) { settings, pro ->
        SettingsUiState(settings, pro)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /** Returns false when the accent needs Pro, so the caller can open the Pro screen. */
    fun setAccent(index: Int): Boolean {
        if (index != 0 && !uiState.value.isPro) return false
        viewModelScope.launch { settingsRepository.setAccentIndex(index) }
        return true
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setSnoozeMinutes(minutes) }
    }

    fun setMissedAfterMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setMissedAfterMinutes(minutes)
            doseSync.run("missed_threshold")
        }
    }
}
