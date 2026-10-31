@file:OptIn(ExperimentalLayoutApi::class)

package app.dozdefteri.ui.medications

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.core.format.TurkishSuffix
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.components.CollapsibleSection
import app.dozdefteri.ui.components.ConfirmDialog
import app.dozdefteri.ui.components.DashedChip
import app.dozdefteri.ui.components.DateSelectDialog
import app.dozdefteri.ui.components.DozDropdownField
import app.dozdefteri.ui.components.DozTextField
import app.dozdefteri.ui.components.DozTopBar
import app.dozdefteri.ui.components.FieldLabel
import app.dozdefteri.ui.components.NavIcon
import app.dozdefteri.ui.components.PrimaryButton
import app.dozdefteri.ui.components.RadioCard
import app.dozdefteri.ui.components.SelectChip
import app.dozdefteri.ui.components.TextAction
import app.dozdefteri.ui.components.TimeSelectDialog
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozIcons
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Forms offered as chips, in design order (OTHER is kept only for older data). */
val FORM_CHOICES = listOf(
    MedicationForm.TABLET,
    MedicationForm.CAPSULE,
    MedicationForm.LIQUID,
    MedicationForm.DROPS,
    MedicationForm.INJECTION,
    MedicationForm.INHALER,
    MedicationForm.CREAM
)

fun formLabelRes(form: MedicationForm): Int = when (form) {
    MedicationForm.TABLET -> R.string.form_tablet
    MedicationForm.CAPSULE -> R.string.form_capsule
    MedicationForm.LIQUID -> R.string.form_liquid
    MedicationForm.DROPS -> R.string.form_drops
    MedicationForm.INJECTION -> R.string.form_injection
    MedicationForm.INHALER -> R.string.form_inhaler
    MedicationForm.CREAM -> R.string.form_cream
    MedicationForm.OTHER -> R.string.form_other
}

/** Unit counted per dose and in stock for each form. */
fun formUnitRes(form: MedicationForm): Int = when (form) {
    MedicationForm.TABLET, MedicationForm.OTHER -> R.string.unit_tablet
    MedicationForm.CAPSULE -> R.string.unit_capsule
    MedicationForm.LIQUID -> R.string.unit_ml
    MedicationForm.DROPS -> R.string.unit_drop
    MedicationForm.INJECTION -> R.string.unit_injection
    MedicationForm.INHALER -> R.string.unit_puff
    MedicationForm.CREAM -> R.string.unit_application
}

@Composable
fun MedicationEditScreen(
    onSaved: () -> Unit,
    onArchived: () -> Unit,
    onBack: () -> Unit,
    viewModel: MedicationEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unitLabels = FORM_CHOICES.plus(MedicationForm.OTHER).associateWith { stringResource(formUnitRes(it)) }
    LaunchedEffect(unitLabels) { viewModel.setUnitLabels(unitLabels) }
    var scrollTarget by remember { mutableStateOf<FormField?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MedicationEditEvent.Saved -> onSaved()
                MedicationEditEvent.Archived -> onArchived()
                is MedicationEditEvent.ScrollTo -> scrollTarget = event.field
            }
        }
    }
    MedicationEditContent(
        state = state,
        scrollTarget = scrollTarget,
        onScrolled = { scrollTarget = null },
        actions = MedicationEditActions(
            onNameChange = viewModel::onNameChange,
            onStrengthChange = viewModel::onStrengthChange,
            onStrengthUnitChange = viewModel::onStrengthUnitChange,
            onFormChange = viewModel::onFormChange,
            onPerDoseChange = viewModel::onPerDoseChange,
            onScheduleTypeChange = viewModel::onScheduleTypeChange,
            onToggleDay = viewModel::onToggleDay,
            onIntervalChange = viewModel::onIntervalChange,
            onToggleTime = viewModel::onToggleTime,
            onAddTime = viewModel::onAddTime,
            onStartDateChange = viewModel::onStartDateChange,
            onDurationChange = viewModel::onDurationChange,
            onIndefiniteChange = viewModel::onIndefiniteChange,
            onStockChange = viewModel::onStockChange,
            onLowStockDaysChange = viewModel::onLowStockDaysChange,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::save,
            onArchive = viewModel::archive,
            onClose = onBack
        )
    )
}

data class MedicationEditActions(
    val onNameChange: (String) -> Unit = {},
    val onStrengthChange: (String) -> Unit = {},
    val onStrengthUnitChange: (String) -> Unit = {},
    val onFormChange: (MedicationForm) -> Unit = {},
    val onPerDoseChange: (String) -> Unit = {},
    val onScheduleTypeChange: (ScheduleType) -> Unit = {},
    val onToggleDay: (DayOfWeek) -> Unit = {},
    val onIntervalChange: (String) -> Unit = {},
    val onToggleTime: (LocalTime) -> Unit = {},
    val onAddTime: (LocalTime) -> Unit = {},
    val onStartDateChange: (LocalDate) -> Unit = {},
    val onDurationChange: (String) -> Unit = {},
    val onIndefiniteChange: (Boolean) -> Unit = {},
    val onStockChange: (String) -> Unit = {},
    val onLowStockDaysChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onArchive: () -> Unit = {},
    val onClose: () -> Unit = {}
)

@Composable
fun MedicationEditContent(
    state: MedicationEditUiState,
    actions: MedicationEditActions,
    scrollTarget: FormField? = null,
    onScrolled: () -> Unit = {}
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    val close: () -> Unit = {
        if (state.dirty) {
            confirmDiscard = true
        } else {
            actions.onClose()
        }
    }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    Scaffold(
        containerColor = Doz.colors.bgApp,
        topBar = {
            DozTopBar(
                title = stringResource(
                    if (state.isEditing) R.string.medication_edit_title else R.string.medication_add_title
                ),
                onNavigate = close,
                navIcon = NavIcon.Close,
                trailing = { TextAction(stringResource(R.string.save), onClick = actions.onSave) }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Doz.colors.primary)
            }
        } else {
            FormBody(state, actions, Modifier.padding(padding), scrollTarget, onScrolled) { confirmArchive = true }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.discard_title),
            text = stringResource(R.string.discard_text),
            confirmLabel = stringResource(R.string.discard_confirm),
            onConfirm = {
                confirmDiscard = false
                actions.onClose()
            },
            onDismiss = { confirmDiscard = false }
        )
    }
    if (confirmArchive) {
        ConfirmDialog(
            title = stringResource(R.string.archive_confirm_title),
            text = stringResource(R.string.archive_confirm_text),
            confirmLabel = stringResource(R.string.archive_move),
            onConfirm = {
                confirmArchive = false
                actions.onArchive()
            },
            onDismiss = { confirmArchive = false }
        )
    }
}

@Composable
private fun FormBody(
    state: MedicationEditUiState,
    actions: MedicationEditActions,
    modifier: Modifier,
    scrollTarget: FormField?,
    onScrolled: () -> Unit,
    onArchive: () -> Unit
) {
    val form = state.form
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val positions = remember { mutableStateMapOf<FormField, Int>() }
    var durationOpen by rememberSaveable { mutableStateOf(false) }
    var stockOpen by rememberSaveable { mutableStateOf(false) }
    var noteOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(scrollTarget) {
        val target = scrollTarget ?: return@LaunchedEffect
        when (target) {
            FormField.DURATION -> durationOpen = true
            FormField.STOCK -> stockOpen = true
            FormField.NOTE -> noteOpen = true
            else -> Unit
        }
        positions[target]?.let { scope.launch { scroll.animateScrollTo(it) } }
        onScrolled()
    }
    fun Modifier.track(field: FormField) =
        onGloballyPositioned { positions[field] = it.positionInParent().y.toInt() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scroll)
            .padding(horizontal = Space.s16),
        verticalArrangement = Arrangement.spacedBy(Space.s24)
    ) {
        Column(Modifier.track(FormField.NAME)) {
            FieldLabel(stringResource(R.string.field_med_name))
            DozTextField(
                value = form.name,
                onValueChange = actions.onNameChange,
                error = errorText(state.errors.name, state.showError(FormField.NAME)),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        }
        Row(Modifier.track(FormField.STRENGTH), horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
            Column(Modifier.weight(1f)) {
                FieldLabel(stringResource(R.string.field_strength))
                DozTextField(
                    value = form.strengthAmount,
                    onValueChange = actions.onStrengthChange,
                    numeric = true,
                    error = errorText(state.errors.strength, state.showError(FormField.STRENGTH))
                )
            }
            Column(Modifier.weight(1f)) {
                FieldLabel(stringResource(R.string.field_strength_unit))
                DozDropdownField(
                    selected = form.strengthUnit,
                    options = MedicationFormValidator.STRENGTH_UNITS,
                    label = { it },
                    onSelect = actions.onStrengthUnitChange
                )
            }
        }
        Column(Modifier.track(FormField.PER_DOSE)) {
            FieldLabel(stringResource(R.string.field_form))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.s8),
                verticalArrangement = Arrangement.spacedBy(Space.s8)
            ) {
                FORM_CHOICES.forEach { choice ->
                    SelectChip(
                        text = stringResource(formLabelRes(choice)),
                        selected = form.form == choice,
                        onClick = { actions.onFormChange(choice) },
                        icon = DozIcons.forForm(choice)
                    )
                }
            }
            PerDoseStepper(form, actions.onPerDoseChange, errorText(state.errors.perDose, true))
        }
        ScheduleSection(state, actions, Modifier.track(FormField.SCHEDULE))
        if (form.scheduleType != ScheduleType.AS_NEEDED) {
            TimesSection(state, actions, Modifier.track(FormField.TIMES))
        }
        SummaryBox(form)
        Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
            DurationSection(
                state,
                actions,
                durationOpen,
                { durationOpen = !durationOpen },
                Modifier.track(FormField.DURATION)
            )
            StockSection(state, actions, stockOpen, { stockOpen = !stockOpen }, Modifier.track(FormField.STOCK))
            CollapsibleSection(
                title = stringResource(R.string.section_note),
                subtitle = form.note.trim().ifEmpty { stringResource(R.string.note_placeholder) },
                expanded = noteOpen,
                onToggle = { noteOpen = !noteOpen },
                modifier = Modifier.track(FormField.NOTE)
            ) {
                DozTextField(
                    value = form.note,
                    onValueChange = actions.onNoteChange,
                    singleLine = false,
                    placeholder = stringResource(R.string.note_placeholder),
                    error = errorText(state.errors.note, true),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = Space.s16),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryButton(
                stringResource(R.string.save),
                onClick = actions.onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.isEditing) {
                TextButton(onClick = onArchive, modifier = Modifier.padding(top = Space.s8)) {
                    Text(
                        stringResource(R.string.archive_move),
                        style = Doz.type.labelLarge,
                        color = Doz.colors.danger.fg
                    )
                }
            }
        }
    }
}

@Composable
private fun PerDoseStepper(form: MedicationFormState, onChange: (String) -> Unit, error: String?) {
    val value = MedicationFormValidator.parseDecimal(form.perDose) ?: 1.0
    val unit = stringResource(formUnitRes(form.form))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s12),
        modifier = Modifier.padding(top = Space.s16)
    ) {
        Text(
            stringResource(R.string.field_per_dose),
            style = Doz.type.bodyLarge,
            color = Doz.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        FilledTonalIconButton(
            onClick = { onChange(format(if (value > 1.0) value - 1.0 else 0.5)) },
            enabled = value > 0.5
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.decrease))
        }
        Text(Formats.dose(value, unit), style = Doz.type.titleMedium, color = Doz.colors.textPrimary)
        FilledTonalIconButton(onClick = { onChange(format(if (value < 1.0) 1.0 else value + 1.0)) }) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.increase))
        }
    }
    if (error != null) Text(error, style = Doz.type.caption, color = Doz.colors.danger.fg)
}

private fun format(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

@Composable
private fun ScheduleSection(state: MedicationEditUiState, actions: MedicationEditActions, modifier: Modifier) {
    val form = state.form
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.s8)) {
        FieldLabel(stringResource(R.string.field_how_often))
        RadioCard(
            title = stringResource(R.string.schedule_daily_title),
            description = stringResource(R.string.schedule_daily_desc),
            selected = form.scheduleType == ScheduleType.DAILY,
            onClick = { actions.onScheduleTypeChange(ScheduleType.DAILY) }
        )
        RadioCard(
            title = stringResource(R.string.schedule_weekdays_title),
            description = stringResource(R.string.schedule_weekdays_desc),
            selected = form.scheduleType == ScheduleType.WEEKDAYS,
            onClick = { actions.onScheduleTypeChange(ScheduleType.WEEKDAYS) }
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.s8),
                verticalArrangement = Arrangement.spacedBy(Space.s8)
            ) {
                DayOfWeek.entries.forEach { day ->
                    SelectChip(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        selected = day in form.days,
                        onClick = { actions.onToggleDay(day) },
                        showCheck = true,
                        role = Role.Checkbox
                    )
                }
            }
            errorText(state.errors.days, state.showError(FormField.SCHEDULE))?.let { ErrorLine(it) }
        }
        RadioCard(
            title = stringResource(R.string.schedule_every_n_title),
            description = stringResource(R.string.schedule_every_n_desc),
            selected = form.scheduleType == ScheduleType.EVERY_N_DAYS,
            onClick = { actions.onScheduleTypeChange(ScheduleType.EVERY_N_DAYS) }
        ) {
            FieldLabel(stringResource(R.string.field_interval_days))
            DozTextField(
                value = form.intervalDays,
                onValueChange = actions.onIntervalChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                error = errorText(state.errors.interval, state.showError(FormField.SCHEDULE))
            )
        }
        RadioCard(
            title = stringResource(R.string.schedule_as_needed_title),
            description = stringResource(R.string.schedule_as_needed_desc),
            selected = form.scheduleType == ScheduleType.AS_NEEDED,
            onClick = { actions.onScheduleTypeChange(ScheduleType.AS_NEEDED) }
        )
    }
}

@Composable
private fun TimesSection(state: MedicationEditUiState, actions: MedicationEditActions, modifier: Modifier) {
    val form = state.form
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val chips = (MedicationFormValidator.SUGGESTED_TIMES + state.extraTimes + form.times).distinct().sorted()
    Column(modifier) {
        FieldLabel(stringResource(R.string.field_which_times))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
            verticalArrangement = Arrangement.spacedBy(Space.s8)
        ) {
            chips.forEach { time ->
                SelectChip(
                    text = Formats.time(time, is24Hour),
                    selected = time in form.times,
                    onClick = { actions.onToggleTime(time) },
                    showCheck = true,
                    role = Role.Checkbox
                )
            }
            DashedChip(stringResource(R.string.add_other_time), Icons.Rounded.Add, onClick = { pickTime = true })
        }
        errorText(state.errors.times, state.showError(FormField.TIMES))?.let { ErrorLine(it) }
    }
    if (pickTime) {
        TimeSelectDialog(
            initial = form.times.lastOrNull()?.plusHours(1) ?: LocalTime.of(9, 0),
            onConfirm = {
                pickTime = false
                actions.onAddTime(it)
            },
            onDismiss = { pickTime = false }
        )
    }
}

@Composable
private fun SummaryBox(form: MedicationFormState) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val texts = SummaryTexts(
        daily = stringResource(R.string.summary_daily),
        weekdays = stringResource(R.string.summary_weekdays),
        everyNDays = stringResource(R.string.summary_every_n),
        asNeeded = stringResource(R.string.summary_as_needed),
        duration = stringResource(R.string.summary_duration),
        indefinite = stringResource(R.string.summary_indefinite),
        and = stringResource(R.string.list_and),
        turkish = TurkishSuffix.isTurkish()
    )
    val dose = Formats.dose(
        MedicationFormValidator.parseDecimal(form.perDose) ?: 1.0,
        stringResource(formUnitRes(form.form))
    )
    val sentence = MedicationSummary.build(
        type = form.scheduleType,
        times = form.times,
        dayNames = DayOfWeek.entries.filter { it in form.days }
            .map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
        intervalDays = form.intervalDays.trim().toIntOrNull(),
        doseText = dose,
        durationDays = if (form.indefinite) null else form.durationDays.trim().toIntOrNull(),
        texts = texts,
        is24Hour = is24Hour
    )
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Space.s12),
        modifier = Modifier
            .fillMaxWidth()
            .background(Doz.colors.primaryContainer, DozShapes.card)
            .padding(Space.s16)
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = Doz.colors.onPrimaryContainer)
        Text(sentence, style = Doz.type.bodyLarge, color = Doz.colors.onPrimaryContainer)
    }
}

@Composable
private fun DurationSection(
    state: MedicationEditUiState,
    actions: MedicationEditActions,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier
) {
    val form = state.form
    var pickStart by rememberSaveable { mutableStateOf(false) }
    val startText = if (form.startDate == state.today) {
        stringResource(R.string.duration_starts_today)
    } else {
        stringResource(
            R.string.duration_starts_on,
            form.startDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
        )
    }
    val lengthText = if (form.indefinite) {
        stringResource(R.string.duration_indefinite)
    } else {
        stringResource(R.string.duration_days, form.durationDays.trim())
    }
    CollapsibleSection(
        title = stringResource(R.string.section_duration),
        subtitle = "$startText · $lengthText",
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier
    ) {
        FieldLabel(stringResource(R.string.field_start_date))
        TextAction(
            form.startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())),
            onClick = { pickStart = true }
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
            Text(
                stringResource(R.string.duration_indefinite_switch),
                style = Doz.type.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = form.indefinite,
                onCheckedChange = actions.onIndefiniteChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Doz.colors.primary)
            )
        }
        if (!form.indefinite) {
            FieldLabel(stringResource(R.string.field_duration_days))
            DozTextField(
                value = form.durationDays,
                onValueChange = actions.onDurationChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                error = errorText(state.errors.duration, true)
            )
        }
    }
    if (pickStart) {
        DateSelectDialog(
            initial = form.startDate,
            onConfirm = {
                pickStart = false
                actions.onStartDateChange(it)
            },
            onDismiss = { pickStart = false }
        )
    }
}

@Composable
private fun StockSection(
    state: MedicationEditUiState,
    actions: MedicationEditActions,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier
) {
    val form = state.form
    val unit = stringResource(formUnitRes(form.form))
    val subtitle = if (form.stockQuantity.isBlank()) {
        stringResource(R.string.stock_not_entered)
    } else {
        val amount = MedicationFormValidator.parseDecimal(form.stockQuantity)?.let { Formats.dose(it, unit) }
            ?: form.stockQuantity
        stringResource(R.string.stock_summary, amount, form.lowStockDays.trim())
    }
    CollapsibleSection(
        title = stringResource(R.string.section_stock),
        subtitle = subtitle,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier
    ) {
        FieldLabel(stringResource(R.string.field_stock_now, unit))
        DozTextField(
            value = form.stockQuantity,
            onValueChange = actions.onStockChange,
            numeric = true,
            error = errorText(state.errors.stock, true)
        )
        FieldLabel(stringResource(R.string.field_low_stock_days))
        DozTextField(
            value = form.lowStockDays,
            onValueChange = actions.onLowStockDaysChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            error = errorText(state.errors.lowStockDays, true)
        )
    }
}

@Composable
private fun ErrorLine(text: String) {
    Text(text, style = Doz.type.caption, color = Doz.colors.danger.fg, modifier = Modifier.padding(top = Space.s4))
}

@Composable
private fun errorText(error: FieldError?, show: Boolean): String? {
    if (error == null || !show) return null
    return when (error) {
        FieldError.NAME_REQUIRED -> stringResource(R.string.error_name_required)
        FieldError.NAME_TOO_LONG -> stringResource(R.string.error_too_long, MedicationFormValidator.NAME_MAX)
        FieldError.STRENGTH_INVALID -> stringResource(R.string.error_amount_invalid)
        FieldError.PER_DOSE_INVALID -> stringResource(R.string.error_amount_invalid)
        FieldError.TIMES_REQUIRED -> stringResource(R.string.error_times_required)
        FieldError.TIMES_TOO_MANY -> stringResource(R.string.error_times_too_many, MedicationFormValidator.TIMES_MAX)
        FieldError.DAYS_REQUIRED -> stringResource(R.string.error_days_required)
        FieldError.INTERVAL_RANGE -> stringResource(
            R.string.error_interval_range,
            MedicationFormValidator.MIN_INTERVAL,
            MedicationFormValidator.MAX_INTERVAL
        )
        FieldError.DURATION_INVALID -> stringResource(R.string.error_duration, MedicationFormValidator.MAX_DURATION)
        FieldError.STOCK_INVALID -> stringResource(R.string.error_number_invalid)
        FieldError.LOW_STOCK_DAYS_INVALID ->
            stringResource(R.string.error_low_stock_days, MedicationFormValidator.MAX_LOW_STOCK_DAYS)
        FieldError.NOTE_TOO_LONG -> stringResource(R.string.error_too_long, MedicationFormValidator.NOTE_MAX)
    }
}

@DozPreviews
@Composable
private fun MedicationEditPreview() {
    val form = MedicationFormState(
        name = "Metformin",
        strengthAmount = "500",
        times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        startDate = LocalDate.of(2026, 11, 1)
    )
    DozDefteriTheme {
        MedicationEditContent(
            state = MedicationEditUiState(
                isLoading = false,
                today = LocalDate.of(2026, 11, 1),
                form = form,
                errors = MedicationFormValidator.validate(form)
            ),
            actions = MedicationEditActions()
        )
    }
}
