package app.dozdefteri.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val doseSync: DoseSync
) : ViewModel() {
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingDone(true)
            doseSync.run("onboarding")
            onDone()
        }
    }
}
