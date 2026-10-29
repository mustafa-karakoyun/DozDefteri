package app.dozdefteri.ui.medications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.data.repository.MedicationRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.domain.DoseGenerator
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.navigation.MedicationEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class MedicationEditUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val form: MedicationFormState,
    val errors: MedicationFormErrors = MedicationFormErrors(),
    /** Fields the user has changed; errors are only shown for these (and always for list-type fields). */
    val touched: Set<String> = emptySet()
) {
    val canSave: Boolean get() = !isLoading && !isSaving && errors.isValid
}

sealed interface MedicationEditEvent {
    data object Saved : MedicationEditEvent
    data object Archived : MedicationEditEvent
}

@HiltViewModel
class MedicationEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val medicationRepository: MedicationRepository,
    private val profileRepository: ProfileRepository,
    private val doseSync: DoseSync,
    private val notifier: DoseNotifier,
    private val clock: Clock
) : ViewModel() {

    private val medicationId: Long? = savedStateHandle.toRoute<MedicationEdit>().medicationId
    private var original: MedicationEntity? = null

    private val _uiState = MutableStateFlow(
        initialState(MedicationFormState(startDate = LocalDate.now(clock)))
    )
    val uiState: StateFlow<MedicationEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<MedicationEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (medicationId == null) {
            _uiState.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch { load(medicationId) }
        }
    }

    private fun initialState(form: MedicationFormState) =
        MedicationEditUiState(form = form, errors = MedicationFormValidator.validate(form))

    private suspend fun load(id: Long) {
        val loaded = medicationRepository.get(id)
        if (loaded == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        original = loaded.medication
        val medication = loaded.medication
        val schedule = loaded.schedule
        val form = MedicationFormState(
            name = medication.name,
            doseAmount = formatNumber(medication.doseAmount),
            doseUnit = medication.doseUnit,
            form = medication.form,
            scheduleType = schedule?.type ?: ScheduleType.DAILY,
            times = schedule?.times ?: emptyList(),
            days = DayOfWeek.entries.filter { DoseGenerator.isDayInMask(schedule?.daysOfWeekMask ?: 0, it) }.toSet(),
            intervalDays = schedule?.intervalDays?.takeIf { schedule.type == ScheduleType.EVERY_N_DAYS }
                ?: MedicationFormValidator.MIN_INTERVAL,
            startDate = schedule?.startDate ?: LocalDate.now(clock),
            endDate = schedule?.endDate,
            stockEnabled = medication.stockQuantity != null,
            stockQuantity = medication.stockQuantity?.let(::formatNumber).orEmpty(),
            lowStockThreshold = medication.lowStockThreshold?.let(::formatNumber).orEmpty(),
            note = medication.note.orEmpty()
        )
        _uiState.value = initialState(form).copy(isLoading = false, isEditing = true)
    }

    fun onNameChange(value: String) = edit("name") { it.copy(name = value) }
    fun onDoseAmountChange(value: String) = edit("doseAmount") { it.copy(doseAmount = value) }
    fun onDoseUnitChange(value: String) = edit("doseUnit") { it.copy(doseUnit = value) }
    fun onFormChange(value: MedicationForm) = edit("form") { it.copy(form = value) }
    fun onScheduleTypeChange(value: ScheduleType) = edit("scheduleType") { it.copy(scheduleType = value) }
    fun onIntervalChange(value: Int) = edit("interval") {
        it.copy(
            intervalDays = value.coerceIn(MedicationFormValidator.MIN_INTERVAL, MedicationFormValidator.MAX_INTERVAL)
        )
    }
    fun onStartDateChange(value: LocalDate) = edit("startDate") { it.copy(startDate = value) }
    fun onEndDateChange(value: LocalDate?) = edit("endDate") { it.copy(endDate = value) }
    fun onStockEnabledChange(value: Boolean) = edit("stockEnabled") { it.copy(stockEnabled = value) }
    fun onStockQuantityChange(value: String) = edit("stockQuantity") { it.copy(stockQuantity = value) }
    fun onThresholdChange(value: String) = edit("lowStockThreshold") { it.copy(lowStockThreshold = value) }
    fun onNoteChange(value: String) = edit("note") { it.copy(note = value) }

    fun onAddTime(time: LocalTime) = edit("times") { state ->
        state.copy(times = (state.times + time.withSecond(0).withNano(0)).distinct().sorted())
    }

    fun onRemoveTime(time: LocalTime) = edit("times") { it.copy(times = it.times - time) }

    fun onToggleDay(day: DayOfWeek) = edit("days") { state ->
        state.copy(days = if (day in state.days) state.days - day else state.days + day)
    }

    private fun edit(field: String, transform: (MedicationFormState) -> MedicationFormState) {
        _uiState.update { state ->
            val form = transform(state.form)
            state.copy(form = form, errors = MedicationFormValidator.validate(form), touched = state.touched + field)
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val profile = original?.profileId ?: profileRepository.getActiveProfile()?.id
            if (profile == null) {
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            val now = clock.instant()
            val (medication, schedule) = buildEntities(state.form, profile, now.toEpochMilli())
            medicationRepository.save(medication, schedule, now)
            doseSync.run("edit")
            _events.send(MedicationEditEvent.Saved)
        }
    }

    fun archive() {
        val id = medicationId ?: return
        viewModelScope.launch {
            val shownIds = medicationRepository.archive(id, clock.instant())
            shownIds.forEach(notifier::cancelDose)
            doseSync.run("archive")
            _events.send(MedicationEditEvent.Archived)
        }
    }

    private fun buildEntities(
        form: MedicationFormState,
        profileId: Long,
        now: Long
    ): Pair<MedicationEntity, ScheduleEntity> {
        val stock = if (form.stockEnabled) MedicationFormValidator.parseDecimal(form.stockQuantity) else null
        val threshold = if (stock != null) MedicationFormValidator.parseDecimal(form.lowStockThreshold) else null
        val base = original
        val medication = MedicationEntity(
            id = base?.id ?: 0,
            profileId = profileId,
            name = form.name.trim(),
            doseAmount = MedicationFormValidator.parseDecimal(form.doseAmount) ?: 1.0,
            doseUnit = form.doseUnit.trim(),
            form = form.form,
            note = form.note.trim().ifEmpty { null },
            stockQuantity = stock,
            lowStockThreshold = threshold,
            lowStockNotified = base?.lowStockNotified ?: false,
            isArchived = base?.isArchived ?: false,
            createdAt = base?.createdAt ?: now,
            updatedAt = now
        )
        val type = form.scheduleType
        val schedule = ScheduleEntity(
            medicationId = medication.id,
            type = type,
            times = if (type == ScheduleType.AS_NEEDED) emptyList() else form.times.distinct().sorted(),
            daysOfWeekMask = if (type == ScheduleType.WEEKDAYS) DoseGenerator.maskOf(form.days) else 0,
            intervalDays = if (type == ScheduleType.EVERY_N_DAYS) form.intervalDays else 1,
            startDate = form.startDate,
            endDate = form.endDate
        )
        return medication to schedule
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
