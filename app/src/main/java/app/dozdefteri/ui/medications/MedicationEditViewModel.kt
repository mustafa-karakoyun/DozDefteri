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
import app.dozdefteri.domain.StockEstimator
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
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class MedicationEditUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val today: LocalDate,
    val form: MedicationFormState,
    val errors: MedicationFormErrors = MedicationFormErrors(),
    /** Errors are shown after the first save attempt, or for fields the user already edited. */
    val showAllErrors: Boolean = false,
    val touched: Set<FormField> = emptySet(),
    /** Custom times added with "+ Başka saat", shown next to the suggested times. */
    val extraTimes: List<LocalTime> = emptyList(),
    val dirty: Boolean = false
) {
    fun showError(field: FormField): Boolean = showAllErrors || field in touched
}

sealed interface MedicationEditEvent {
    data object Saved : MedicationEditEvent
    data object Archived : MedicationEditEvent
    data class ScrollTo(val field: FormField) : MedicationEditEvent
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

    /** Localized stock unit per form ("tablet", "kapsül", "damla"…), provided by the screen. */
    private var unitLabels: Map<MedicationForm, String> = emptyMap()

    private val _uiState = MutableStateFlow(initialState(MedicationFormState(startDate = LocalDate.now(clock))))
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

    fun setUnitLabels(labels: Map<MedicationForm, String>) {
        unitLabels = labels
    }

    private fun initialState(form: MedicationFormState) = MedicationEditUiState(
        today = LocalDate.now(clock),
        form = form,
        errors = MedicationFormValidator.validate(form)
    )

    private suspend fun load(id: Long) {
        val loaded = medicationRepository.get(id)
        if (loaded == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        val medication = loaded.medication
        val schedule = loaded.schedule
        original = medication
        val start = schedule?.startDate ?: LocalDate.now(clock)
        val end = schedule?.endDate
        val times = schedule?.times.orEmpty()
        val form = MedicationFormState(
            name = medication.name,
            strengthAmount = medication.strengthAmount?.let(::formatNumber).orEmpty(),
            strengthUnit = medication.strengthUnit ?: MedicationFormValidator.STRENGTH_UNITS.first(),
            form = medication.form,
            perDose = formatNumber(medication.doseAmount),
            scheduleType = schedule?.type ?: ScheduleType.DAILY,
            times = times,
            days = DayOfWeek.entries.filter { DoseGenerator.isDayInMask(schedule?.daysOfWeekMask ?: 0, it) }.toSet(),
            intervalDays = (schedule?.intervalDays?.takeIf { schedule.type == ScheduleType.EVERY_N_DAYS } ?: 3)
                .toString(),
            startDate = start,
            durationDays = end?.let { (ChronoUnit.DAYS.between(start, it) + 1).toString() }
                ?: MedicationFormValidator.DEFAULT_DURATION.toString(),
            indefinite = end == null,
            stockQuantity = medication.stockQuantity?.let(::formatNumber).orEmpty(),
            lowStockDays = (medication.lowStockDays ?: MedicationFormValidator.DEFAULT_LOW_STOCK_DAYS).toString(),
            note = medication.note.orEmpty()
        )
        _uiState.value = initialState(form).copy(
            isLoading = false,
            isEditing = true,
            extraTimes = times.filterNot { it in MedicationFormValidator.SUGGESTED_TIMES }
        )
    }

    fun onNameChange(value: String) = edit(FormField.NAME) { it.copy(name = value) }
    fun onStrengthChange(value: String) = edit(FormField.STRENGTH) { it.copy(strengthAmount = value) }
    fun onStrengthUnitChange(value: String) = edit(FormField.STRENGTH) { it.copy(strengthUnit = value) }
    fun onFormChange(value: MedicationForm) = edit(FormField.PER_DOSE) { it.copy(form = value) }
    fun onPerDoseChange(value: String) = edit(FormField.PER_DOSE) { it.copy(perDose = value) }
    fun onScheduleTypeChange(value: ScheduleType) = edit(FormField.SCHEDULE) { it.copy(scheduleType = value) }
    fun onIntervalChange(value: String) = edit(FormField.SCHEDULE) { it.copy(intervalDays = value) }
    fun onStartDateChange(value: LocalDate) = edit(FormField.DURATION) { it.copy(startDate = value) }
    fun onDurationChange(value: String) = edit(FormField.DURATION) { it.copy(durationDays = value) }
    fun onIndefiniteChange(value: Boolean) = edit(FormField.DURATION) { it.copy(indefinite = value) }
    fun onStockChange(value: String) = edit(FormField.STOCK) { it.copy(stockQuantity = value) }
    fun onLowStockDaysChange(value: String) = edit(FormField.STOCK) { it.copy(lowStockDays = value) }
    fun onNoteChange(value: String) = edit(FormField.NOTE) { it.copy(note = value) }

    fun onToggleDay(day: DayOfWeek) = edit(FormField.SCHEDULE) { state ->
        state.copy(days = if (day in state.days) state.days - day else state.days + day)
    }

    /** Selects or unselects one of the time chips (suggested or custom). */
    fun onToggleTime(time: LocalTime) = edit(FormField.TIMES) { state ->
        state.copy(times = if (time in state.times) state.times - time else (state.times + time).sorted())
    }

    /** "+ Başka saat": adds a custom chip, selected. */
    fun onAddTime(time: LocalTime) {
        val clean = time.withSecond(0).withNano(0)
        _uiState.update { state ->
            val extras = if (clean in MedicationFormValidator.SUGGESTED_TIMES || clean in state.extraTimes) {
                state.extraTimes
            } else {
                (state.extraTimes + clean).sorted()
            }
            state.copy(extraTimes = extras)
        }
        edit(FormField.TIMES) { state ->
            if (clean in state.times) state else state.copy(times = (state.times + clean).sorted())
        }
    }

    private fun edit(field: FormField, transform: (MedicationFormState) -> MedicationFormState) {
        _uiState.update { state ->
            val form = transform(state.form)
            state.copy(
                form = form,
                errors = MedicationFormValidator.validate(form),
                touched = state.touched + field,
                dirty = true
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving) return
        val firstInvalid = state.errors.firstInvalid
        if (firstInvalid != null) {
            _uiState.update { it.copy(showAllErrors = true) }
            viewModelScope.launch { _events.send(MedicationEditEvent.ScrollTo(firstInvalid)) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val profileId = original?.profileId ?: profileRepository.getActiveProfile()?.id
            if (profileId == null) {
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            val now = clock.instant()
            val (medication, schedule) = buildEntities(state.form, profileId, now.toEpochMilli())
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
        val type = form.scheduleType
        val schedule = ScheduleEntity(
            medicationId = original?.id ?: 0,
            type = type,
            times = if (type == ScheduleType.AS_NEEDED) emptyList() else form.times.distinct().sorted(),
            daysOfWeekMask = if (type == ScheduleType.WEEKDAYS) DoseGenerator.maskOf(form.days) else 0,
            intervalDays = if (type == ScheduleType.EVERY_N_DAYS) form.intervalDays.trim().toInt() else 1,
            startDate = form.startDate,
            endDate = form.endDate
        )
        val perDose = MedicationFormValidator.parseDecimal(form.perDose) ?: 1.0
        val stock = form.stockQuantity.takeIf { it.isNotBlank() }?.let(MedicationFormValidator::parseDecimal)
        val lowDays = if (stock != null) form.lowStockDays.trim().toIntOrNull() else null
        val dailyAverage = StockEstimator.dailyAverage(listOf(schedule), perDose)
        // The alarm logic works on an amount; convert "warn N days before running out" into one.
        val threshold = if (lowDays != null && dailyAverage > 0.0) lowDays * dailyAverage else null
        val base = original
        val medication = MedicationEntity(
            id = base?.id ?: 0,
            profileId = profileId,
            name = form.name.trim(),
            doseAmount = perDose,
            doseUnit = unitLabels[form.form] ?: base?.doseUnit ?: form.form.name.lowercase(),
            form = form.form,
            note = form.note.trim().ifEmpty { null },
            stockQuantity = stock,
            lowStockThreshold = threshold,
            lowStockNotified = base?.lowStockNotified ?: false,
            isArchived = base?.isArchived ?: false,
            createdAt = base?.createdAt ?: now,
            updatedAt = now,
            strengthAmount = form.strengthAmount.takeIf { it.isNotBlank() }
                ?.let(MedicationFormValidator::parseDecimal),
            strengthUnit = form.strengthUnit.takeIf { form.strengthAmount.isNotBlank() },
            lowStockDays = lowDays
        )
        return medication to schedule
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
