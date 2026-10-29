@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package app.dozdefteri.ui.today

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.components.EmptyState
import app.dozdefteri.ui.components.PermissionSnapshot
import app.dozdefteri.ui.components.SystemSettings
import app.dozdefteri.ui.components.WarningBanner
import app.dozdefteri.ui.components.rememberPermissionSnapshot
import app.dozdefteri.ui.components.statusVisual
import app.dozdefteri.ui.medications.MedicationListItem
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.StatusMissed
import app.dozdefteri.ui.theme.profileColor
import java.time.LocalTime

data class TodayActions(
    val onTake: (Long) -> Unit = {},
    val onSkip: (Long) -> Unit = {},
    val onSnooze: (Long) -> Unit = {},
    val onUndo: (Long) -> Unit = {},
    val onTakeAsNeeded: (Long) -> Unit = {},
    val onSelectProfile: (Long) -> Unit = {},
    val onAddMedication: () -> Unit = {},
    val onFixExactAlarms: () -> Unit = {},
    val onFixNotifications: () -> Unit = {}
)

@Composable
fun TodayScreen(onAddMedication: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberPermissionSnapshot()
    val context = LocalContext.current
    TodayContent(
        state = state,
        permissions = permissions,
        actions = TodayActions(
            onTake = viewModel::take,
            onSkip = viewModel::skip,
            onSnooze = viewModel::snooze,
            onUndo = viewModel::undo,
            onTakeAsNeeded = viewModel::takeAsNeeded,
            onSelectProfile = viewModel::selectProfile,
            onAddMedication = onAddMedication,
            onFixExactAlarms = { SystemSettings.openExactAlarmSettings(context) },
            onFixNotifications = { SystemSettings.openNotificationSettings(context) }
        )
    )
}

@Composable
fun TodayContent(state: TodayUiState, permissions: PermissionSnapshot, actions: TodayActions) {
    Scaffold(
        topBar = { TopAppBar(title = { ProfileTitle(state, actions.onSelectProfile) }) },
        floatingActionButton = {
            if (state.hasMedications) {
                ExtendedFloatingActionButton(
                    onClick = actions.onAddMedication,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_medication)) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!permissions.exactAlarmsAllowed) {
                item(key = "warn_exact") {
                    WarningBanner(
                        text = stringResource(R.string.warning_exact_alarms),
                        actionLabel = stringResource(R.string.fix),
                        onAction = actions.onFixExactAlarms
                    )
                }
            }
            if (!permissions.notificationsAllowed) {
                item(key = "warn_notifications") {
                    WarningBanner(
                        text = stringResource(R.string.warning_notifications_off),
                        actionLabel = stringResource(R.string.turn_on),
                        onAction = actions.onFixNotifications
                    )
                }
            }
            if (state.isLoading) return@LazyColumn

            if (!state.hasMedications) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.Medication,
                        text = stringResource(R.string.empty_medications),
                        actionLabel = stringResource(R.string.add_medication),
                        onAction = actions.onAddMedication
                    )
                }
                return@LazyColumn
            }

            if (state.lowStock.isNotEmpty()) {
                item(key = "low_stock") { LowStockCard(state.lowStock) }
            }

            if (state.groups.isEmpty()) {
                item(key = "no_doses") {
                    EmptyState(icon = Icons.Filled.EventAvailable, text = stringResource(R.string.no_doses_today))
                }
            }
            state.groups.forEach { (time, doses) ->
                item(key = "time_$time") { TimeHeader(time) }
                items(doses, key = { "dose_${it.id}" }) { dose -> DoseRow(dose, actions) }
            }

            if (state.asNeeded.isNotEmpty()) {
                item(key = "as_needed_header") {
                    SectionHeader(stringResource(R.string.section_as_needed))
                }
                items(state.asNeeded, key = { "as_needed_${it.medication.id}" }) { item ->
                    AsNeededRow(item, onTake = { actions.onTakeAsNeeded(item.medication.id) })
                }
            }
        }
    }
}

@Composable
private fun ProfileTitle(state: TodayUiState, onSelect: (Long) -> Unit) {
    val profile = state.profile
    val title = profile?.name ?: stringResource(R.string.tab_today)
    if (state.profiles.size <= 1 || profile == null) {
        Text(title)
        return
    }
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(
                Icons.Filled.Circle,
                contentDescription = null,
                tint = profileColor(profile.colorIndex),
                modifier = Modifier.size(12.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.switch_profile))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.profiles.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    leadingIcon = {
                        Icon(Icons.Filled.Circle, contentDescription = null, tint = profileColor(option.colorIndex))
                    },
                    onClick = {
                        expanded = false
                        onSelect(option.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun TimeHeader(time: LocalTime) {
    val context = LocalContext.current
    SectionHeader(Formats.time(time, DateFormat.is24HourFormat(context)))
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 12.dp)
            .semantics { heading() }
    )
}

@Composable
private fun DoseRow(dose: TodayDose, actions: TodayActions) {
    val details = dose.details
    val visual = statusVisual(dose.status)
    val snoozeLabel = stringResource(R.string.action_snooze)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = if (dose.isOpen) ({ actions.onSnooze(dose.id) }) else null,
                onLongClickLabel = if (dose.isOpen) snoozeLabel else null
            )
            .semantics {
                if (dose.isOpen) {
                    customActions = listOf(
                        CustomAccessibilityAction(snoozeLabel) {
                            actions.onSnooze(dose.id)
                            true
                        }
                    )
                }
            }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(visual.icon, contentDescription = null, tint = visual.color)
                Column(Modifier.weight(1f)) {
                    Text(details.medicationName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        Formats.dose(details.event.quantity, details.doseUnit),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(visual.label), style = MaterialTheme.typography.labelLarge, color = visual.color)
                    if (dose.isOverdue) {
                        Text(
                            stringResource(R.string.overdue),
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusMissed
                        )
                    }
                }
            }
            if (dose.isOpen) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { actions.onTake(dose.id) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.action_taken)) }
                    OutlinedButton(
                        onClick = { actions.onSkip(dose.id) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.action_skip)) }
                }
                Text(
                    stringResource(R.string.long_press_to_snooze),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (dose.canUndo) {
                TextButton(onClick = { actions.onUndo(dose.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.undo))
                }
            }
        }
    }
}

@Composable
private fun AsNeededRow(item: MedicationListItem, onTake: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.medication.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    Formats.dose(item.medication.doseAmount, item.medication.doseUnit),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            FilledTonalButton(onClick = onTake, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.take_now))
            }
        }
    }
}

@Composable
private fun LowStockCard(items: List<MedicationListItem>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = StatusMissed)
                Text(
                    stringResource(R.string.low_stock_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )
            }
            items.forEach { item ->
                val stock = item.medication.stockQuantity ?: 0.0
                val text = buildString {
                    append(item.medication.name)
                    append(": ")
                    append(stringResource(R.string.stock_left, Formats.dose(stock, item.medication.doseUnit)))
                    item.remainingDays?.let { days ->
                        append(" · ")
                        append(pluralStringResource(R.plurals.days_left, days, days))
                    }
                }
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun TodayPreview() {
    fun dose(id: Long, status: DoseStatus, hour: Int) = TodayDose(
        details = DoseEventDetails(
            event = DoseEventEntity(
                id = id,
                scheduleId = 1,
                medicationId = 1,
                profileId = 1,
                originalScheduledAt = 0,
                scheduledAt = 0,
                status = status,
                notifiedAt = null,
                actionAt = null,
                quantity = 1.0
            ),
            medicationName = "Vitamin D",
            doseAmount = 1.0,
            doseUnit = "tablet",
            scheduleType = ScheduleType.DAILY,
            profileName = "Me"
        ),
        time = LocalTime.of(hour, 0),
        isOverdue = status == DoseStatus.PENDING,
        canUndo = status == DoseStatus.TAKEN
    )
    DozDefteriTheme {
        TodayContent(
            state = TodayUiState(
                isLoading = false,
                profile = ProfileEntity(id = 1, name = "Me", colorIndex = 0, createdAt = 0),
                hasMedications = true,
                groups = listOf(
                    LocalTime.of(8, 0) to listOf(dose(1, DoseStatus.TAKEN, 8)),
                    LocalTime.of(20, 0) to listOf(dose(2, DoseStatus.PENDING, 20))
                )
            ),
            permissions = PermissionSnapshot(notificationsAllowed = true, exactAlarmsAllowed = false),
            actions = TodayActions()
        )
    }
}
