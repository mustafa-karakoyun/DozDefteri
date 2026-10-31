package app.dozdefteri.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.billing.BillingManager
import app.dozdefteri.billing.PriceState
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
    val isPro: Boolean = false,
    val price: PriceState = PriceState.Loading
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val doseSync: DoseSync,
    billingManager: BillingManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        billingManager.isPro,
        billingManager.price
    ) { settings, pro, price ->
        SettingsUiState(settings, pro, price)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        billingManager.start()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setLargeText(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLargeText(enabled) }
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
