package app.dozdefteri.ui.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.data.repository.MedicationRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.StockEstimator
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.today.TodayViewModel
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
    /** Estimated whole days of stock left; null when not tracked, as-needed or not estimable. */
    val remainingDays: Int?,
    val isLowStock: Boolean,
    /** Stock bar fill, 0..1 (a full bar is a month of doses). */
    val stockFraction: Float
) {
    val isAsNeeded: Boolean get() = schedule?.type == ScheduleType.AS_NEEDED
}

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

    fun startStockTracking(medicationId: Long, amount: Double) {
        viewModelScope.launch {
            medicationRepository.startStockTracking(
                medicationId,
                amount,
                MedicationFormValidator.DEFAULT_LOW_STOCK_DAYS,
                clock.instant()
            )
        }
    }

    fun addStock(medicationId: Long, amount: Double) {
        viewModelScope.launch { medicationRepository.addStock(medicationId, amount, clock.instant()) }
    }
}

internal fun MedicationEntity.toItem(schedules: List<ScheduleEntity>): MedicationListItem {
    val stock = stockQuantity
    val threshold = lowStockThreshold
    val days = StockEstimator.remainingDays(stock, schedules, doseAmount)
    return MedicationListItem(
        medication = this,
        schedule = schedules.firstOrNull(),
        remainingDays = days,
        isLowStock = stock != null && threshold != null && stock <= threshold,
        stockFraction = stock?.let { TodayViewModel.stockFraction(days, it) } ?: 0f
    )
}
