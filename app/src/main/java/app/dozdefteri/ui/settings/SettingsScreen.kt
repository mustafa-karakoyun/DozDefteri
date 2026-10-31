package app.dozdefteri.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.BuildConfig
import app.dozdefteri.R
import app.dozdefteri.billing.PriceState
import app.dozdefteri.core.locale.AppLanguage
import app.dozdefteri.data.prefs.SettingsRepository
import app.dozdefteri.domain.model.ThemeMode
import app.dozdefteri.ui.components.PermissionSnapshot
import app.dozdefteri.ui.components.RowTrailing
import app.dozdefteri.ui.components.ScreenHeader
import app.dozdefteri.ui.components.SettingsRow
import app.dozdefteri.ui.components.SystemSettings
import app.dozdefteri.ui.components.rememberPermissionSnapshot
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

data class SettingsActions(
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onLargeText: (Boolean) -> Unit = {},
    val onLanguage: (AppLanguage) -> Unit = {},
    val onSnooze: (Int) -> Unit = {},
    val onMissedAfter: (Int) -> Unit = {},
    val onFixNotifications: () -> Unit = {},
    val onFixExactAlarms: () -> Unit = {},
    val onFixBattery: () -> Unit = {},
    val onReports: () -> Unit = {},
    val onProfiles: () -> Unit = {},
    val onPro: () -> Unit = {},
    val onPrivacy: (() -> Unit)? = null,
    val onCreateTestDose: () -> Unit = {}
)

@Composable
fun SettingsScreen(
    onReports: () -> Unit,
    onProfiles: () -> Unit,
    onPro: () -> Unit,
    onBatteryGuide: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    debugViewModel: DebugViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberPermissionSnapshot()
    val context = LocalContext.current
    val testName = stringResource(R.string.debug_test_medication_name)
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    var language by remember { mutableStateOf(AppLanguage.current(context)) }
    SettingsContent(
        state = state,
        permissions = permissions,
        language = language.takeIf { AppLanguage.isSupported },
        actions = SettingsActions(
            onThemeMode = viewModel::setThemeMode,
            onLargeText = viewModel::setLargeText,
            onLanguage = {
                language = it
                AppLanguage.set(context, it)
            },
            onSnooze = viewModel::setSnoozeMinutes,
            onMissedAfter = viewModel::setMissedAfterMinutes,
            onFixNotifications = { SystemSettings.openNotificationSettings(context) },
            onFixExactAlarms = { SystemSettings.openExactAlarmSettings(context) },
            onFixBattery = onBatteryGuide,
            onReports = onReports,
            onProfiles = onProfiles,
            onPro = onPro,
            onPrivacy = privacyUrl.takeIf { it.isNotBlank() }?.let { url ->
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
fun SettingsContent(
    state: SettingsUiState,
    permissions: PermissionSnapshot,
    language: AppLanguage?,
    actions: SettingsActions
) {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val settings = state.settings
    Scaffold(
        containerColor = Doz.colors.bgApp,
        topBar = { ScreenHeader(stringResource(R.string.tab_settings), profile = null) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s16),
            verticalArrangement = Arrangement.spacedBy(Space.s8)
        ) {
            HealthCard(permissions, actions)

            Group(stringResource(R.string.settings_appearance)) {
                if (language != null) {
                    SettingsRow(
                        stringResource(R.string.settings_language),
                        subtitle = stringResource(languageLabel(language)),
                        onClick = { dialog = DIALOG_LANGUAGE }
                    )
                }
                SettingsRow(
                    stringResource(R.string.settings_theme),
                    subtitle = stringResource(themeLabel(settings.themeMode)),
                    onClick = { dialog = DIALOG_THEME }
                )
                SettingsRow(
                    stringResource(R.string.settings_large_text),
                    subtitle = stringResource(R.string.settings_large_text_desc),
                    trailing = RowTrailing.Toggle(settings.largeText, actions.onLargeText)
                )
            }

            Group(stringResource(R.string.settings_reminders)) {
                SettingsRow(
                    stringResource(R.string.settings_snooze),
                    subtitle = pluralStringResource(R.plurals.minutes, settings.snoozeMinutes, settings.snoozeMinutes),
                    onClick = { dialog = DIALOG_SNOOZE }
                )
                SettingsRow(
                    stringResource(R.string.settings_missed_after_v2),
                    subtitle = stringResource(R.string.settings_missed_after_value, settings.missedAfterMinutes),
                    onClick = { dialog = DIALOG_MISSED }
                )
            }

            Group(stringResource(R.string.settings_data_profiles)) {
                SettingsRow(
                    stringResource(R.string.reports_title),
                    subtitle = stringResource(R.string.settings_reports_desc),
                    onClick = actions.onReports
                )
                SettingsRow(
                    stringResource(R.string.profiles_title),
                    subtitle = stringResource(R.string.settings_profiles_desc),
                    trailing = if (state.isPro) RowTrailing.Chevron else RowTrailing.ProBadge,
                    onClick = actions.onProfiles
                )
                SettingsRow(
                    stringResource(R.string.pro_title),
                    subtitle = proSubtitle(state),
                    icon = Icons.Rounded.StarOutline,
                    iconTint = Doz.colors.pro.fg,
                    onClick = actions.onPro
                )
            }

            Group(stringResource(R.string.settings_about)) {
                SettingsRow(
                    stringResource(R.string.settings_privacy),
                    subtitle = stringResource(R.string.settings_privacy_desc),
                    trailing = if (actions.onPrivacy != null) RowTrailing.Chevron else RowTrailing.None,
                    onClick = actions.onPrivacy
                )
                SettingsRow(
                    stringResource(R.string.settings_version),
                    subtitle = BuildConfig.VERSION_NAME,
                    trailing = RowTrailing.None
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.s12),
                modifier = Modifier.padding(vertical = Space.s16, horizontal = Space.s8)
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = Doz.colors.textSecondary)
                Text(
                    stringResource(R.string.disclaimer_short),
                    style = Doz.type.bodyMedium,
                    color = Doz.colors.textSecondary
                )
            }

            if (BuildConfig.DEBUG) DeveloperSection(onCreateTestDose = actions.onCreateTestDose)
        }
    }

    when (dialog) {
        DIALOG_LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = AppLanguage.entries.map { it to stringResource(languageLabel(it)) },
            selected = language ?: AppLanguage.SYSTEM,
            onSelect = {
                dialog = null
                actions.onLanguage(it)
            },
            onDismiss = { dialog = null }
        )
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
            title = stringResource(R.string.settings_missed_after_v2),
            options = SettingsRepository.MISSED_AFTER_OPTIONS.map {
                it to stringResource(R.string.settings_missed_after_value, it)
            },
            selected = settings.missedAfterMinutes,
            onSelect = {
                dialog = null
                actions.onMissedAfter(it)
            },
            onDismiss = { dialog = null }
        )
    }
}

private const val DIALOG_LANGUAGE = "language"
private const val DIALOG_THEME = "theme"
private const val DIALOG_SNOOZE = "snooze"
private const val DIALOG_MISSED = "missed"

@Composable
private fun proSubtitle(state: SettingsUiState): String = when {
    state.isPro -> stringResource(R.string.settings_pro_active)
    state.price is PriceState.Available -> stringResource(R.string.settings_pro_price, state.price.formattedPrice)
    else -> stringResource(R.string.settings_pro_one_time)
}

/** "Hatırlatma sağlığı": every permission with icon + text; problems get a one-tap "Düzelt". */
@Composable
private fun HealthCard(permissions: PermissionSnapshot, actions: SettingsActions) {
    val c = Doz.colors
    val issues = permissions.issueCount
    Surface(
        shape = DozShapes.card,
        color = c.surfaceDefault,
        border = BorderStroke(if (issues > 0) 2.dp else 1.dp, if (issues > 0) c.warning.fg else c.borderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = Space.s8)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s12),
                modifier = Modifier.padding(horizontal = Space.s16, vertical = Space.s8)
            ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = c.textPrimary)
                Text(
                    stringResource(R.string.health_title),
                    style = Doz.type.titleMedium,
                    color = c.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
                if (issues > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.s4),
                        modifier = Modifier
                            .background(c.warning.bg, DozShapes.pill)
                            .padding(horizontal = Space.s8, vertical = Space.s4)
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = c.warning.fg,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            pluralStringResource(R.plurals.health_issues, issues, issues),
                            style = Doz.type.labelMedium,
                            color = c.warning.fg
                        )
                    }
                }
            }
            HealthRow(
                ok = permissions.notificationsAllowed,
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.health_notifications),
                okText = stringResource(R.string.health_notifications_on),
                problemText = stringResource(R.string.health_notifications_off),
                onFix = actions.onFixNotifications
            )
            HealthRow(
                ok = permissions.exactAlarmsAllowed,
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.health_exact),
                okText = stringResource(R.string.health_exact_on),
                problemText = stringResource(R.string.health_exact_off),
                onFix = actions.onFixExactAlarms
            )
            HealthRow(
                ok = permissions.batteryUnrestricted,
                icon = Icons.Outlined.BatteryStd,
                title = stringResource(R.string.health_battery),
                okText = stringResource(R.string.health_battery_ok),
                problemText = stringResource(R.string.health_battery_restricted),
                onFix = actions.onFixBattery
            )
        }
    }
}

@Composable
private fun HealthRow(
    ok: Boolean,
    icon: ImageVector,
    title: String,
    okText: String,
    problemText: String,
    onFix: () -> Unit
) {
    SettingsRow(
        title = title,
        subtitle = if (ok) okText else problemText,
        icon = icon,
        iconTint = if (ok) Doz.colors.textSecondary else Doz.colors.warning.fg,
        trailing = if (ok) RowTrailing.Check else RowTrailing.Action(stringResource(R.string.fix), onFix)
    )
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = Doz.type.titleMedium,
        color = Doz.colors.textSecondary,
        modifier = Modifier
            .padding(top = Space.s16, bottom = Space.s4)
            .semantics { heading() }
    )
    Surface(
        shape = DozShapes.card,
        color = Doz.colors.surfaceDefault,
        border = BorderStroke(1.dp, Doz.colors.borderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = Space.s4), content = content)
    }
}

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun languageLabel(language: AppLanguage): Int = when (language) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.TURKISH -> R.string.language_turkish
    AppLanguage.ENGLISH -> R.string.language_english
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
        containerColor = Doz.colors.surfaceDefault,
        title = { Text(title, style = Doz.type.titleLarge) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Doz.touch.min)
                            .selectable(selected = value == selected, role = Role.RadioButton) { onSelect(value) }
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Doz.colors.primary)
                        )
                        Text(label, style = Doz.type.bodyLarge, modifier = Modifier.padding(start = Space.s12))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@DozPreviews
@Composable
private fun SettingsPreview() {
    DozDefteriTheme {
        SettingsContent(
            state = SettingsUiState(price = PriceState.Available("₺149")),
            permissions = PermissionSnapshot(notificationsAllowed = true, exactAlarmsAllowed = false),
            language = AppLanguage.TURKISH,
            actions = SettingsActions()
        )
    }
}
