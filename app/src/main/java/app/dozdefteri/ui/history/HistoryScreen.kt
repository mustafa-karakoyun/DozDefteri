@file:OptIn(ExperimentalMaterial3Api::class)

package app.dozdefteri.ui.history

import android.text.format.DateFormat
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.domain.DayStatus
import app.dozdefteri.ui.components.statusVisual
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.StatusMissed
import app.dozdefteri.ui.theme.StatusPartial
import app.dozdefteri.ui.theme.StatusTaken
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

data class HistoryActions(
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onSelectDay: (LocalDate?) -> Unit = {}
)

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        actions = HistoryActions(
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onSelectDay = viewModel::selectDay
        )
    )
}

@Composable
fun HistoryContent(state: HistoryUiState, actions: HistoryActions) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_history)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonthSelector(state.month, actions)
            Text(
                stringResource(
                    R.string.monthly_adherence,
                    state.adherencePercent?.let { localizedPercent(it) } ?: stringResource(R.string.no_value)
                ),
                style = MaterialTheme.typography.titleMedium
            )
            CalendarGrid(state, onSelect = { actions.onSelectDay(it) })
            Legend()
        }
    }
    val selected = state.selectedDay
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { actions.onSelectDay(null) }) {
            DayDetails(selected, state.dosesByDay[selected].orEmpty())
        }
    }
}

@Composable
private fun localizedPercent(value: Int): String = stringResource(R.string.percent_value, value)

@Composable
private fun MonthSelector(month: YearMonth, actions: HistoryActions) {
    val title = month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = actions.onPreviousMonth) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_month)
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        IconButton(onClick = actions.onNextMonth) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_month)
            )
        }
    }
}

@Composable
private fun CalendarGrid(state: HistoryUiState, onSelect: (LocalDate) -> Unit) {
    val locale = Locale.getDefault()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val weekDays = (0 until 7).map { firstDayOfWeek.plus(it.toLong()) }
    val firstOfMonth = state.month.atDay(1)
    val leadingBlanks = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val days = (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }
    val cells: List<LocalDate?> = List(leadingBlanks) { null } + days
    val rows = cells.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        rows.forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { index ->
                    val date = week.getOrNull(index)
                    Box(Modifier.weight(1f)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                status = state.dayStatuses[date] ?: DayStatus.NONE,
                                isToday = date == state.today,
                                onClick = { onSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DayVisual(val icon: ImageVector?, val color: Color, val label: Int)

@Composable
private fun dayVisual(status: DayStatus): DayVisual = when (status) {
    DayStatus.ALL_TAKEN -> DayVisual(Icons.Filled.CheckCircle, StatusTaken, R.string.day_all_taken)
    DayStatus.PARTIAL -> DayVisual(Icons.Filled.Contrast, StatusPartial, R.string.day_partial)
    DayStatus.MISSED -> DayVisual(Icons.Filled.Cancel, StatusMissed, R.string.day_missed)
    DayStatus.NONE -> DayVisual(null, MaterialTheme.colorScheme.onSurfaceVariant, R.string.day_no_doses)
}

@Composable
private fun DayCell(date: LocalDate, status: DayStatus, isToday: Boolean, onClick: () -> Unit) {
    val visual = dayVisual(status)
    val description = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + ", " +
        stringResource(visual.label)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .heightIn(min = 48.dp)
            .then(
                if (isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
    ) {
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium)
        if (visual.icon != null) {
            Icon(visual.icon, contentDescription = null, tint = visual.color, modifier = Modifier.size(16.dp))
        } else {
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(DayStatus.ALL_TAKEN, DayStatus.PARTIAL, DayStatus.MISSED).forEach { status ->
            val visual = dayVisual(status)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                visual.icon?.let { Icon(it, contentDescription = null, tint = visual.color, modifier = Modifier.size(16.dp)) }
                Text(stringResource(visual.label), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DayDetails(day: LocalDate, doses: List<DoseEventDetails>) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )
        if (doses.isEmpty()) {
            Text(stringResource(R.string.day_no_doses), style = MaterialTheme.typography.bodyMedium)
        }
        doses.forEach { dose ->
            val visual = statusVisual(dose.event.status)
            val time = Instant.ofEpochMilli(dose.event.originalScheduledAt).atZone(ZoneId.systemDefault()).toLocalTime()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(Formats.time(time, is24Hour), style = MaterialTheme.typography.labelLarge)
                Column(Modifier.weight(1f)) {
                    Text(dose.medicationName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        Formats.dose(dose.event.quantity, dose.doseUnit),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(visual.icon, contentDescription = null, tint = visual.color)
                Text(stringResource(visual.label), color = visual.color, style = MaterialTheme.typography.labelLarge)
            }
            HorizontalDivider()
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun HistoryPreview() {
    val month = YearMonth.of(2026, 11)
    DozDefteriTheme {
        HistoryContent(
            state = HistoryUiState(
                isLoading = false,
                month = month,
                today = month.atDay(10),
                adherencePercent = 86,
                dayStatuses = mapOf(
                    month.atDay(1) to DayStatus.ALL_TAKEN,
                    month.atDay(2) to DayStatus.PARTIAL,
                    month.atDay(3) to DayStatus.MISSED
                )
            ),
            actions = HistoryActions()
        )
    }
}
