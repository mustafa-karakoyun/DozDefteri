package app.dozdefteri.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.core.debug.DebugDoseCreator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Debug-only actions (visible only when BuildConfig.DEBUG is true). */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val debugDoseCreator: DebugDoseCreator,
    private val doseSync: DoseSync
) : ViewModel() {
    fun createTestDose(medicationName: String) {
        viewModelScope.launch {
            if (debugDoseCreator.createTestDose(medicationName)) doseSync.run("debug")
        }
    }
}
