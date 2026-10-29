package app.dozdefteri.ui.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.data.repository.MedicationRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.StockEstimator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class MedicationListItem(
    val medication: MedicationEntity,
    val schedule: ScheduleEntity?,
    /** Estimated whole days of stock left; null when not tracked or not estimable. */
    val remainingDays: Int?,
    val isLowStock: Boolean
)

data class MedicationsUiState(
    val isLoading: Boolean = true,
    val active: List<MedicationListItem> = emptyList(),
    val archived: List<MedicationListItem> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MedicationsViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val medicationRepository: MedicationRepository,
    private val doseSync: DoseSync,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<MedicationsUiState> = profileRepository.activeProfile
        .flatMapLatest { profile ->
            if (profile == null) {
                flowOf(MedicationsUiState(isLoading = true))
            } else {
                combine(
                    medicationRepository.observeMedications(profile.id, archived = false),
                    medicationRepository.observeMedications(profile.id, archived = true),
                    medicationRepository.observeSchedules()
                ) { active, archived, schedules ->
                    val byMedication = schedules.groupBy { it.medicationId }
                    MedicationsUiState(
                        isLoading = false,
                        active = active.map { it.toItem(byMedication[it.id].orEmpty()) },
                        archived = archived.map { it.toItem(byMedication[it.id].orEmpty()) }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicationsUiState())

    fun restore(medicationId: Long) {
        viewModelScope.launch {
            medicationRepository.unarchive(medicationId, clock.instant())
            doseSync.run("unarchive")
        }
    }
}

internal fun MedicationEntity.toItem(schedules: List<ScheduleEntity>): MedicationListItem {
    val stock = stockQuantity
    val threshold = lowStockThreshold
    return MedicationListItem(
        medication = this,
        schedule = schedules.firstOrNull(),
        remainingDays = StockEstimator.remainingDays(stock, schedules, doseAmount),
        isLowStock = stock != null && threshold != null && stock <= threshold
    )
}
