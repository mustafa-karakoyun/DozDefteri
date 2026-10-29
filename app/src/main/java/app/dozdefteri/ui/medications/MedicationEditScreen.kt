@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.dozdefteri.ui.medications

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.core.format.ScheduleFormatter
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.components.ConfirmDialog
import app.dozdefteri.ui.components.DateSelectDialog
import app.dozdefteri.ui.components.TimeSelectDialog
import app.dozdefteri.ui.theme.DozDefteriTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MedicationEditScreen(
    onSaved: () -> Unit,
    onArchived: () -> Unit,
    onBack: () -> Unit,
    viewModel: MedicationEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MedicationEditEvent.Saved -> onSaved()
                MedicationEditEvent.Archived -> onArchived()
            }
        }
    }
    MedicationEditContent(
        state = state,
        actions = MedicationEditActions(
            onNameChange = viewModel::onNameChange,
            onDoseAmountChange = viewModel::onDoseAmountChange,
            onDoseUnitChange = viewModel::onDoseUnitChange,
            onFormChange = viewModel::onFormChange,
            onScheduleTypeChange = viewModel::onScheduleTypeChange,
            onAddTime = viewModel::onAddTime,
            onRemoveTime = viewModel::onRemoveTime,
            onToggleDay = viewModel::onToggleDay,
            onIntervalChange = viewModel::onIntervalChange,
            onStartDateChange = viewModel::onStartDateChange,
            onEndDateChange = viewModel::onEndDateChange,
            onStockEnabledChange = viewModel::onStockEnabledChange,
            onStockQuantityChange = viewModel::onStockQuantityChange,
            onThresholdChange = viewModel::onThresholdChange,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::save,
            onArchive = viewModel::archive,
            onBack = onBack
        )
    )
}

data class MedicationEditActions(
    val onNameChange: (String) -> Unit = {},
    val onDoseAmountChange: (String) -> Unit = {},
    val onDoseUnitChange: (String) -> Unit = {},
    val onFormChange: (MedicationForm) -> Unit = {},
    val onScheduleTypeChange: (ScheduleType) -> Unit = {},
    val onAddTime: (LocalTime) -> Unit = {},
    val onRemoveTime: (LocalTime) -> Unit = {},
    val onToggleDay: (DayOfWeek) -> Unit = {},
    val onIntervalChange: (Int) -> Unit = {},
    val onStartDateChange: (LocalDate) -> Unit = {},
    val onEndDateChange: (LocalDate?) -> Unit = {},
    val onStockEnabledChange: (Boolean) -> Unit = {},
    val onStockQuantityChange: (String) -> Unit = {},
    val onThresholdChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onArchive: () -> Unit = {},
    val onBack: () -> Unit = {}
)

@Composable
fun MedicationEditContent(state: MedicationEditUiState, actions: MedicationEditActions) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.medication_edit_title else R.string.medication_add_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.isEditing) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.archive)) },
                                    onClick = {
                                        menuOpen = false
                                        confirmArchive = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            FormBody(state, actions, padding)
        }
    }

    if (confirmArchive) {
        ConfirmDialog(
            title = stringResource(R.string.archive_confirm_title),
            text = stringResource(R.string.archive_confirm_text),
            confirmLabel = stringResource(R.string.archive),
            onConfirm = {
                confirmArchive = false
                actions.onArchive()
            },
            onDismiss = { confirmArchive = false }
        )
    }
}

@Composable
private fun FormBody(state: MedicationEditUiState, actions: MedicationEditActions, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BasicsSection(state, actions)
        HorizontalDivider()
        ScheduleSection(state, actions)
        HorizontalDivider()
        StockSection(state, actions)
        OutlinedTextField(
            value = state.form.note,
            onValueChange = actions.onNoteChange,
            label = { Text(stringResource(R.string.field_note)) },
            isError = state.errors.note != null,
            supportingText = {
                Text(
                    state.errors.note?.let { errorText(it) }
                        ?: stringResource(
                            R.string.character_count,
                            state.form.note.length,
                            MedicationFormValidator.NOTE_MAX
                        )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = actions.onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
private fun BasicsSection(state: MedicationEditUiState, actions: MedicationEditActions) {
    val form = state.form
    val errors = state.errors
    OutlinedTextField(
        value = form.name,
        onValueChange = actions.onNameChange,
        label = { Text(stringResource(R.string.field_name)) },
        singleLine = true,
        isError = "name" in state.touched && errors.name != null,
        supportingText = errorSupport(errors.name, "name" in state.touched),
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = form.doseAmount,
            onValueChange = actions.onDoseAmountChange,
            label = { Text(stringResource(R.string.field_dose_amount)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = errors.doseAmount != null,
            supportingText = errorSupport(errors.doseAmount),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = form.doseUnit,
            onValueChange = actions.onDoseUnitChange,
            label = { Text(stringResource(R.string.field_dose_unit)) },
            singleLine = true,
            isError = "doseUnit" in state.touched && errors.doseUnit != null,
            supportingText = errorSupport(errors.doseUnit, "doseUnit" in state.touched),
            modifier = Modifier.weight(1f)
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        unitSuggestions().forEach { unit ->
            SuggestionChip(onClick = { actions.onDoseUnitChange(unit) }, label = { Text(unit) })
        }
    }
    FormDropdown(selected = form.form, onSelect = actions.onFormChange)
}

@Composable
private fun unitSuggestions(): List<String> = listOf(
    stringResource(R.string.unit_tablet),
    stringResource(R.string.unit_capsule),
    stringResource(R.string.unit_mg),
    stringResource(R.string.unit_ml),
    stringResource(R.string.unit_drop),
    stringResource(R.string.unit_puff)
)

@Composable
private fun FormDropdown(selected: MedicationForm, onSelect: (MedicationForm) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(stringResource(R.string.field_form), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(stringResource(ScheduleFormatter.formLabelRes(selected)), modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                MedicationForm.entries.forEach { form ->
                    DropdownMenuItem(
                        text = { Text(stringResource(ScheduleFormatter.formLabelRes(form))) },
                        onClick = {
                            expanded = false
                            onSelect(form)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleSection(state: MedicationEditUiState, actions: MedicationEditActions) {
    val form = state.form
    val context = LocalContext.current
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }
    val is24Hour = DateFormat.is24HourFormat(context)
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    SectionTitle(stringResource(R.string.section_schedule))
    Column(Modifier.selectableGroup()) {
        ScheduleType.entries.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = form.scheduleType == type,
                        onClick = { actions.onScheduleTypeChange(type) },
                        role = Role.RadioButton
                    )
            ) {
                RadioButton(selected = form.scheduleType == type, onClick = null)
                Text(stringResource(scheduleTypeLabel(type)), modifier = Modifier.padding(start = 12.dp))
            }
        }
    }

    if (form.scheduleType == ScheduleType.WEEKDAYS) {
        Text(stringResource(R.string.field_days), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { day ->
                val selected = day in form.days
                FilterChip(
                    selected = selected,
                    onClick = { actions.onToggleDay(day) },
                    label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    }
                )
            }
        }
        state.errors.days?.let { ErrorLine(errorText(it)) }
    }

    if (form.scheduleType == ScheduleType.EVERY_N_DAYS) {
        Text(stringResource(R.string.field_interval), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalIconButton(
                onClick = { actions.onIntervalChange(form.intervalDays - 1) },
                enabled = form.intervalDays > MedicationFormValidator.MIN_INTERVAL
            ) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.decrease))
            }
            Text(
                pluralInterval(form.intervalDays),
                style = MaterialTheme.typography.titleMedium
            )
            FilledTonalIconButton(
                onClick = { actions.onIntervalChange(form.intervalDays + 1) },
                enabled = form.intervalDays < MedicationFormValidator.MAX_INTERVAL
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.increase))
            }
        }
    }

    if (form.scheduleType != ScheduleType.AS_NEEDED) {
        Text(stringResource(R.string.field_times), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            form.times.forEach { time ->
                val label = Formats.time(time, is24Hour)
                InputChip(
                    selected = false,
                    onClick = { actions.onRemoveTime(time) },
                    label = { Text(label) },
                    trailingIcon = {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_time, label))
                    }
                )
            }
            if (form.times.size < MedicationFormValidator.TIMES_MAX) {
                AssistChip(
                    onClick = { showTimePicker = true },
                    label = { Text(stringResource(R.string.add_time)) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
            }
        }
        state.errors.times?.let { ErrorLine(errorText(it)) }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.field_start_date), style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(form.startDate.format(dateFormatter))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.field_end_date), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(form.endDate?.format(dateFormatter) ?: stringResource(R.string.no_end_date))
                }
                if (form.endDate != null) {
                    IconButton(onClick = { actions.onEndDateChange(null) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_end_date))
                    }
                }
            }
        }
    }
    state.errors.endDate?.let { ErrorLine(errorText(it)) }

    if (showTimePicker) {
        TimeSelectDialog(
            initial = form.times.lastOrNull()?.plusHours(1) ?: LocalTime.of(8, 0),
            onConfirm = {
                showTimePicker = false
                actions.onAddTime(it)
            },
            onDismiss = { showTimePicker = false }
        )
    }
    if (showStartPicker) {
        DateSelectDialog(
            initial = form.startDate,
            onConfirm = {
                showStartPicker = false
                actions.onStartDateChange(it)
            },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        DateSelectDialog(
            initial = form.endDate ?: form.startDate,
            onConfirm = {
                showEndPicker = false
                actions.onEndDateChange(it)
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
private fun StockSection(state: MedicationEditUiState, actions: MedicationEditActions) {
    val form = state.form
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = form.stockEnabled, onValueChange = actions.onStockEnabledChange, role = Role.Switch)
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.field_stock_tracking), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.field_stock_tracking_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = form.stockEnabled, onCheckedChange = null)
    }
    if (form.stockEnabled) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = form.stockQuantity,
                onValueChange = actions.onStockQuantityChange,
                label = { Text(stringResource(R.string.field_stock_quantity)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.errors.stockQuantity != null,
                supportingText = errorSupport(state.errors.stockQuantity),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = form.lowStockThreshold,
                onValueChange = actions.onThresholdChange,
                label = { Text(stringResource(R.string.field_low_stock_threshold)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.errors.lowStockThreshold != null,
                supportingText = {
                    Text(
                        state.errors.lowStockThreshold?.let { errorText(it) }
                            ?: stringResource(R.string.field_low_stock_threshold_hint)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() }
    )
}

private fun errorSupport(error: FieldError?, show: Boolean = true): (@Composable () -> Unit)? =
    if (error != null && show) {
        { Text(errorText(error)) }
    } else {
        null
    }

@Composable
private fun ErrorLine(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun pluralInterval(days: Int): String =
    pluralStringResource(R.plurals.every_n_days, days, days)

fun scheduleTypeLabel(type: ScheduleType): Int = when (type) {
    ScheduleType.DAILY -> R.string.schedule_type_daily
    ScheduleType.WEEKDAYS -> R.string.schedule_type_weekdays
    ScheduleType.EVERY_N_DAYS -> R.string.schedule_type_every_n_days
    ScheduleType.AS_NEEDED -> R.string.schedule_type_as_needed
}

@Composable
private fun errorText(error: FieldError): String = when (error) {
    FieldError.NAME_REQUIRED -> stringResource(R.string.error_name_required)
    FieldError.NAME_TOO_LONG -> stringResource(R.string.error_too_long, MedicationFormValidator.NAME_MAX)
    FieldError.AMOUNT_INVALID -> stringResource(R.string.error_amount_invalid)
    FieldError.UNIT_REQUIRED -> stringResource(R.string.error_unit_required)
    FieldError.UNIT_TOO_LONG -> stringResource(R.string.error_too_long, MedicationFormValidator.UNIT_MAX)
    FieldError.TIMES_REQUIRED -> stringResource(R.string.error_times_required)
    FieldError.TIMES_TOO_MANY -> stringResource(R.string.error_times_too_many, MedicationFormValidator.TIMES_MAX)
    FieldError.DAYS_REQUIRED -> stringResource(R.string.error_days_required)
    FieldError.INTERVAL_RANGE -> stringResource(
        R.string.error_interval_range,
        MedicationFormValidator.MIN_INTERVAL,
        MedicationFormValidator.MAX_INTERVAL
    )
    FieldError.END_BEFORE_START -> stringResource(R.string.error_end_before_start)
    FieldError.STOCK_INVALID -> stringResource(R.string.error_number_invalid)
    FieldError.THRESHOLD_INVALID -> stringResource(R.string.error_number_invalid)
    FieldError.NOTE_TOO_LONG -> stringResource(R.string.error_too_long, MedicationFormValidator.NOTE_MAX)
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun MedicationEditPreview() {
    val form = MedicationFormState(
        name = "Vitamin D",
        doseUnit = "tablet",
        times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        startDate = LocalDate.of(2026, 11, 1),
        stockEnabled = true,
        stockQuantity = "30",
        lowStockThreshold = "5"
    )
    DozDefteriTheme {
        MedicationEditContent(
            state = MedicationEditUiState(
                isLoading = false,
                form = form,
                errors = MedicationFormValidator.validate(form)
            ),
            actions = MedicationEditActions()
        )
    }
}
