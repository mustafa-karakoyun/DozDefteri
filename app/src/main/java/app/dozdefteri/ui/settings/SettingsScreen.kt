@file:OptIn(ExperimentalMaterial3Api::class)

package app.dozdefteri.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.BuildConfig
import app.dozdefteri.R
import app.dozdefteri.data.prefs.SettingsRepository
import app.dozdefteri.domain.model.ThemeMode
import app.dozdefteri.ui.components.PermissionSnapshot
import app.dozdefteri.ui.components.SystemSettings
import app.dozdefteri.ui.components.rememberPermissionSnapshot
import app.dozdefteri.ui.theme.AccentSeeds
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.StatusMissed
import app.dozdefteri.ui.theme.StatusTaken

data class SettingsActions(
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onAccent: (Int) -> Unit = {},
    val onSnooze: (Int) -> Unit = {},
    val onMissedAfter: (Int) -> Unit = {},
    val onFixExactAlarms: () -> Unit = {},
    val onNotificationSettings: () -> Unit = {},
    val onBatteryGuide: () -> Unit = {},
    val onExport: () -> Unit = {},
    val onProfiles: () -> Unit = {},
    val onPro: () -> Unit = {},
    val onPrivacyPolicy: (() -> Unit)? = null,
    val onCreateTestDose: () -> Unit = {}
)

@Composable
fun SettingsScreen(
    onBatteryGuide: () -> Unit,
    onExport: () -> Unit,
    onProfiles: () -> Unit,
    onPro: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    debugViewModel: DebugViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberPermissionSnapshot()
    val context = LocalContext.current
    val testName = stringResource(R.string.debug_test_medication_name)
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    SettingsContent(
        state = state,
        permissions = permissions,
        actions = SettingsActions(
            onThemeMode = viewModel::setThemeMode,
            onAccent = { if (!viewModel.setAccent(it)) onPro() },
            onSnooze = viewModel::setSnoozeMinutes,
            onMissedAfter = viewModel::setMissedAfterMinutes,
            onFixExactAlarms = { SystemSettings.openExactAlarmSettings(context) },
            onNotificationSettings = { SystemSettings.openNotificationSettings(context) },
            onBatteryGuide = onBatteryGuide,
            onExport = onExport,
            onProfiles = onProfiles,
            onPro = onPro,
            onPrivacyPolicy = privacyUrl.takeIf { it.isNotBlank() }?.let { url ->
                {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: ActivityNotFoundException) {
                        // No browser installed.
                    }
                }
            },
            onCreateTestDose = { debugViewModel.createTestDose(testName) }
        )
    )
}

@Composable
fun SettingsContent(state: SettingsUiState, permissions: PermissionSnapshot, actions: SettingsActions) {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val settings = state.settings

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_appearance))
            SettingRow(
                title = stringResource(R.string.settings_theme),
                value = stringResource(themeLabel(settings.themeMode)),
                onClick = { dialog = DIALOG_THEME }
            )
            AccentRow(selected = state.effectiveAccent, isPro = state.isPro, onSelect = actions.onAccent)
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_reminders))
            SettingRow(
                title = stringResource(R.string.settings_snooze),
                value = pluralStringResource(R.plurals.minutes, settings.snoozeMinutes, settings.snoozeMinutes),
                onClick = { dialog = DIALOG_SNOOZE }
            )
            SettingRow(
                title = stringResource(R.string.settings_missed_after),
                value = durationLabel(settings.missedAfterMinutes),
                onClick = { dialog = DIALOG_MISSED }
            )
            SettingRow(
                title = stringResource(R.string.settings_exact_alarms),
                value = stringResource(
                    if (permissions.exactAlarmsAllowed) R.string.settings_allowed else R.string.settings_not_allowed
                ),
                icon = if (permissions.exactAlarmsAllowed) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                iconTint = if (permissions.exactAlarmsAllowed) StatusTaken else StatusMissed,
                onClick = actions.onFixExactAlarms
            )
            SettingRow(
                title = stringResource(R.string.settings_notifications),
                value = stringResource(
                    if (permissions.notificationsAllowed) R.string.settings_allowed else R.string.settings_not_allowed
                ),
                icon = if (permissions.notificationsAllowed) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                iconTint = if (permissions.notificationsAllowed) StatusTaken else StatusMissed,
                onClick = actions.onNotificationSettings
            )
            SettingRow(title = stringResource(R.string.battery_guide_title), onClick = actions.onBatteryGuide)
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_data))
            SettingRow(title = stringResource(R.string.export_title), onClick = actions.onExport)
            SettingRow(title = stringResource(R.string.profiles_title), onClick = actions.onProfiles)
            HorizontalDivider()

            SectionHeader(stringResource(R.string.pro_title))
            SettingRow(
                title = stringResource(R.string.settings_pro_status),
                value = stringResource(
                    if (state.isPro) R.string.settings_pro_active else R.string.settings_pro_inactive
                ),
                onClick = actions.onPro
            )
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_about))
            SettingRow(
                title = stringResource(R.string.settings_version),
                value = BuildConfig.VERSION_NAME,
                onClick = null
            )
            actions.onPrivacyPolicy?.let { open ->
                SettingRow(title = stringResource(R.string.settings_privacy_policy), onClick = open)
            }
            Text(
                stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            if (BuildConfig.DEBUG) {
                HorizontalDivider()
                DeveloperSection(onCreateTestDose = actions.onCreateTestDose)
            }
        }
    }

    when (dialog) {
        DIALOG_THEME -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to stringResource(themeLabel(it)) },
            selected = settings.themeMode,
            onSelect = {
                dialog = null
                actions.onThemeMode(it)
            },
            onDismiss = { dialog = null }
        )
        DIALOG_SNOOZE -> ChoiceDialog(
            title = stringResource(R.string.settings_snooze),
            options = SettingsRepository.SNOOZE_OPTIONS.map { it to pluralStringResource(R.plurals.minutes, it, it) },
            selected = settings.snoozeMinutes,
            onSelect = {
                dialog = null
                actions.onSnooze(it)
            },
            onDismiss = { dialog = null }
        )
        DIALOG_MISSED -> ChoiceDialog(
            title = stringResource(R.string.settings_missed_after),
            options = SettingsRepository.MISSED_AFTER_OPTIONS.map { it to durationLabel(it) },
            selected = settings.missedAfterMinutes,
            onSelect = {
                dialog = null
                actions.onMissedAfter(it)
            },
            onDismiss = { dialog = null }
        )
    }
}

private const val DIALOG_THEME = "theme"
private const val DIALOG_SNOOZE = "snooze"
private const val DIALOG_MISSED = "missed"

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun durationLabel(minutes: Int): String =
    if (minutes % 60 == 0) {
        pluralStringResource(R.plurals.hours, minutes / 60, minutes / 60)
    } else {
        pluralStringResource(R.plurals.minutes, minutes, minutes)
    }

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() }
    )
}

@Composable
private fun SettingRow(
    title: String,
    onClick: (() -> Unit)?,
    value: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (value != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun AccentRow(selected: Int, isPro: Boolean, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.bodyLarge)
        if (!isPro) {
            Text(
                stringResource(R.string.settings_accent_pro_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(Modifier.selectableGroup().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccentSeeds.forEachIndexed { index, color ->
                val locked = index != 0 && !isPro
                val description = stringResource(accentName(index)) +
                    if (locked) ", " + stringResource(R.string.pro_required) else ""
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .selectable(
                            selected = selected == index,
                            onClick = { onSelect(index) },
                            role = Role.RadioButton
                        )
                        .semantics { contentDescription = description }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(color, CircleShape)
                            .then(
                                if (selected == index) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        when {
                            selected == index -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                            locked -> Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun accentName(index: Int): Int = when (index) {
    1 -> R.string.accent_indigo
    2 -> R.string.accent_orange
    3 -> R.string.accent_green
    else -> R.string.accent_teal
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = value == selected,
                                onClick = { onSelect(value) },
                                role = Role.RadioButton
                            )
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun SettingsPreview() {
    DozDefteriTheme {
        SettingsContent(
            state = SettingsUiState(),
            permissions = PermissionSnapshot(notificationsAllowed = true, exactAlarmsAllowed = false),
            actions = SettingsActions()
        )
    }
}
