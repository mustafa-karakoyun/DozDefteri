package app.dozdefteri.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.data.prefs.SettingsRepository
import app.dozdefteri.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainUiState(
    val onboardingDone: Boolean,
    val themeMode: ThemeMode,
    val accentIndex: Int
)

/** App-wide state needed before the first screen is drawn: start destination and theme. */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    /** null until DataStore has been read once. */
    val uiState: StateFlow<MainUiState?> = settingsRepository.settings
        .map { settings ->
            MainUiState(
                onboardingDone = settings.onboardingDone,
                themeMode = settings.themeMode,
                accentIndex = settings.accentIndex
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
