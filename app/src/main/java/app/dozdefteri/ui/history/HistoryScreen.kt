package app.dozdefteri.ui.history

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.core.format.TurkishSuffix
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.domain.DayStatus
import app.dozdefteri.domain.DoseCounts
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.ui.components.ChipKind
import app.dozdefteri.ui.components.HeaderProfile
import app.dozdefteri.ui.components.ScreenHeader
import app.dozdefteri.ui.components.StatusChip
import app.dozdefteri.ui.components.TakenTimeSheet
import app.dozdefteri.ui.shell.LocalHeaderProfile
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class HistoryActions(
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onSelectDay: (LocalDate) -> Unit = {},
    val onMarkTakenAt: (DoseEventDetails, LocalTime) -> Unit = { _, _ -> },
    val onMarkSkipped: (Long) -> Unit = {}
)

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        header = LocalHeaderProfile.current,
        actions = HistoryActions(
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onSelectDay = viewModel::selectDay,
            onMarkTakenAt = viewModel::markTakenAt,
            onMarkSkipped = viewModel::markSkipped
        )
    )
}

@Composable
fun HistoryContent(state: HistoryUiState, header: HeaderProfile?, actions: HistoryActions) {
    var editing by remember { mutableStateOf<DoseEventDetails?>(null) }
    Scaffold(
        containerColor = Doz.colors.bgApp,
        topBar = { ScreenHeader(stringResource(R.string.tab_history), header) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s16, vertical = Space.s8),
            verticalArrangement = Arrangement.spacedBy(Space.s16)
        ) {
            MonthNavigator(state.month, actions)
            SummaryCard(state.summary)
            CalendarGrid(state, actions.onSelectDay)
            Legend()
            state.selectedDay?.let { day ->
                DayDetail(day, state.dosesByDay[day].orEmpty(), onEdit = { editing = it })
            }
        }
    }
    editing?.let { details ->
        val zone = ZoneId.systemDefault()
        val planned = Instant.ofEpochMilli(details.event.originalScheduledAt).atZone(zone).toLocalTime()
        val taken = details.event.actionAt?.takeIf { details.event.status == DoseStatus.TAKEN }
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
        TakenTimeSheet(
            medicationName = details.medicationName,
            initial = taken ?: planned,
            onSave = { time ->
                editing = null
                actions.onMarkTakenAt(details, time)
            },
            onDismiss = { editing = null },
            onSkip = {
                editing = null
                actions.onMarkSkipped(details.event.id)
            }
        )
    }
}

@Composable
private fun MonthNavigator(month: YearMonth, actions: HistoryActions) {
    val title = month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = actions.onPreviousMonth) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_month)
            )
        }
        Text(
            title,
            style = Doz.type.titleLarge,
            color = Doz.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        IconButton(onClick = actions.onNextMonth) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_month)
            )
        }
    }
}

@Composable
private fun SummaryCard(summary: DoseCounts) {
    val c = Doz.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.primaryContainer, DozShapes.card)
            .padding(Space.s16),
        verticalArrangement = Arrangement.spacedBy(Space.s4)
    ) {
        Text(
            stringResource(
                R.string.history_adherence,
                summary.percent?.let { stringResource(R.string.percent_value, it) }
                    ?: stringResource(R.string.no_value)
            ),
            style = Doz.type.titleLarge,
            color = c.onPrimaryContainer
        )
        if (summary.planned > 0) {
            Text(
                pluralStringResource(
                    R.plurals.history_counts,
                    summary.planned,
                    summary.planned,
                    TurkishSuffix.countPossessive(summary.taken),
                    summary.skipped,
                    summary.missed
                ),
                style = Doz.type.bodyMedium,
                color = c.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarGrid(state: HistoryUiState, onSelect: (LocalDate) -> Unit) {
    val locale = Locale.getDefault()
    val first = state.month.atDay(1)
    val leading = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val days = (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }
    val cells: List<LocalDate?> = List(leading) { null } + days
    Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.SHORT, locale),
                    style = Doz.type.caption,
                    color = Doz.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { }
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
                (0 until 7).forEach { index ->
                    Box(Modifier.weight(1f)) {
                        week.getOrNull(index)?.let { date ->
                            DayCell(
                                date = date,
                                status = if (date.isAfter(state.today)) {
                                    DayStatus.NONE
                                } else {
                                    state.dayStatuses[date] ?: DayStatus.NONE
                                },
                                counts = state.dayCounts[date] ?: DoseCounts(),
                                selected = date == state.selectedDay,
                                onClick = { onSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class CellStyle(val bg: Color?, val fg: Color, val icon: ImageVector?)

@Composable
private fun cellStyle(status: DayStatus): CellStyle {
    val c = Doz.colors
    return when (status) {
        DayStatus.ALL_TAKEN -> CellStyle(c.taken.bg, c.taken.fg, Icons.Rounded.Check)
        DayStatus.PARTIAL -> CellStyle(c.snoozed.bg, c.snoozed.fg, Icons.Rounded.Contrast)
        DayStatus.MISSED -> CellStyle(c.missed.bg, c.missed.fg, Icons.Rounded.ErrorOutline)
        DayStatus.NONE -> CellStyle(null, c.textSecondary, null)
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: DayStatus,
    counts: DoseCounts,
    selected: Boolean,
    onClick: () -> Unit
) {
    val style = cellStyle(status)
    val description = dayDescription(date, counts)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .heightIn(min = 48.dp)
            .then(if (style.bg != null) Modifier.background(style.bg, DozShapes.small) else Modifier)
            .then(if (selected) Modifier.border(2.dp, Doz.colors.primary, DozShapes.small) else Modifier)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                this.selected = selected
            }
    ) {
        Text(date.dayOfMonth.toString(), style = Doz.type.labelMedium, color = Doz.colors.textPrimary)
        if (style.icon != null) {
            Icon(style.icon, contentDescription = null, tint = style.fg, modifier = Modifier.size(18.dp))
        } else {
            Text("–", style = Doz.type.caption, color = style.fg)
        }
    }
}

/** "14 Ekim, 5 dozdan 4'ü alındı, 1 işaretlenmedi" */
@Composable
private fun dayDescription(date: LocalDate, counts: DoseCounts): String {
    val dateText = date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
    if (counts.total == 0) return stringResource(R.string.day_a11y_none, dateText)
    val parts = mutableListOf(
        pluralStringResource(
            R.plurals.day_progress,
            counts.total,
            counts.total,
            TurkishSuffix.countPossessive(counts.taken)
        )
    )
    if (counts.missed > 0) parts += stringResource(R.string.day_a11y_missed, counts.missed)
    if (counts.skipped > 0) parts += stringResource(R.string.day_a11y_skipped, counts.skipped)
    return (listOf(dateText) + parts).joinToString(", ")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.s16),
        verticalArrangement = Arrangement.spacedBy(Space.s8)
    ) {
        LegendItem(cellStyle(DayStatus.ALL_TAKEN), stringResource(R.string.legend_all_taken))
        LegendItem(cellStyle(DayStatus.PARTIAL), stringResource(R.string.legend_partial))
        LegendItem(cellStyle(DayStatus.MISSED), stringResource(R.string.legend_missed))
        LegendItem(cellStyle(DayStatus.NONE), stringResource(R.string.legend_none))
    }
}

@Composable
private fun LegendItem(style: CellStyle, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
        if (style.icon != null) {
            Icon(style.icon, contentDescription = null, tint = style.fg, modifier = Modifier.size(16.dp))
        } else {
            Text("–", style = Doz.type.caption, color = style.fg)
        }
        Text(label, style = Doz.type.caption, color = Doz.colors.textSecondary)
    }
}

@Composable
private fun DayDetail(day: LocalDate, doses: List<DoseEventDetails>, onEdit: (DoseEventDetails) -> Unit) {
    val c = Doz.colors
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val zone = ZoneId.systemDefault()
    Surface(
        shape = DozShapes.card,
        color = c.surfaceDefault,
        border = BorderStroke(1.dp, c.borderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Space.s16)) {
            Text(
                day.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.getDefault())),
                style = Doz.type.titleMedium,
                color = c.textPrimary,
                modifier = Modifier
                    .padding(bottom = Space.s8)
                    .semantics { heading() }
            )
            if (doses.isEmpty()) {
                Text(stringResource(R.string.day_no_doses), style = Doz.type.bodyMedium, color = c.textSecondary)
            }
            doses.forEachIndexed { index, dose ->
                if (index > 0) HorizontalDivider(color = c.borderDefault)
                val planned = Instant.ofEpochMilli(dose.event.originalScheduledAt).atZone(zone).toLocalTime()
                val actual = actualTimeText(dose, zone, is24Hour)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s16),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(vertical = Space.s8)
                ) {
                    Text(Formats.time(planned, is24Hour), style = Doz.type.doseTime, color = c.textPrimary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                        Text(dose.medicationName, style = Doz.type.titleMedium, color = c.textPrimary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Space.s8),
                            verticalArrangement = Arrangement.spacedBy(Space.s4),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusChip(ChipKind.Dose(dose.event.status))
                            if (actual != null) Text(actual, style = Doz.type.caption, color = c.textSecondary)
                        }
                    }
                    IconButton(onClick = { onEdit(dose) }) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit_dose, dose.medicationName),
                            tint = c.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun actualTimeText(dose: DoseEventDetails, zone: ZoneId, is24Hour: Boolean): String? {
    val actionAt = dose.event.actionAt
    return when (dose.event.status) {
        DoseStatus.MISSED -> stringResource(R.string.not_marked)
        DoseStatus.TAKEN, DoseStatus.SKIPPED -> actionAt?.let {
            val time = Instant.ofEpochMilli(it).atZone(zone).toLocalTime()
            val formatted = Formats.time(time, is24Hour)
            if (TurkishSuffix.isTurkish()) {
                TurkishSuffix.timeLocative(formatted, time)
            } else {
                stringResource(R.string.taken_at, formatted)
            }
        }
        DoseStatus.PENDING, DoseStatus.SNOOZED -> null
    }
}

@DozPreviews
@Composable
private fun HistoryPreview() {
    val month = YearMonth.of(2026, 10)
    DozDefteriTheme {
        HistoryContent(
            state = HistoryUiState(
                isLoading = false,
                month = month,
                today = month.atDay(31),
                summary = DoseCounts(taken = 136, skipped = 5, missed = 7),
                dayStatuses = (1..31).associate {
                    month.atDay(it) to if (it % 9 == 0) DayStatus.PARTIAL else DayStatus.ALL_TAKEN
                } + mapOf(month.atDay(14) to DayStatus.MISSED),
                selectedDay = month.atDay(14)
            ),
            header = HeaderProfile("Ayşe", 0) {},
            actions = HistoryActions()
        )
    }
}
