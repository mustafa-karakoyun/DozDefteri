package app.dozdefteri.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.dozdefteri.R
import app.dozdefteri.core.locale.AppLanguage
import app.dozdefteri.ui.battery.BatterySteps
import app.dozdefteri.ui.battery.Manufacturer
import app.dozdefteri.ui.components.PermissionSnapshot
import app.dozdefteri.ui.components.PrimaryButton
import app.dozdefteri.ui.components.SystemSettings
import app.dozdefteri.ui.components.TextAction
import app.dozdefteri.ui.components.TonalButton
import app.dozdefteri.ui.components.rememberPermissionSnapshot
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozIcons
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Radius
import app.dozdefteri.ui.theme.Space
import kotlinx.coroutines.launch
import java.util.Locale

private enum class OnboardingPage { WELCOME, NOTIFICATIONS, ON_TIME }

data class OnboardingActions(
    val onRequestNotifications: () -> Unit = {},
    val onOpenExactAlarmSettings: () -> Unit = {},
    val onOpenBatterySettings: () -> Unit = {},
    val onSwitchLanguage: (() -> Unit)? = null,
    val onFinish: () -> Unit = {}
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val permissions = rememberPermissionSnapshot()
    var advanceAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Granted or not, move on: a refusal is never blamed and Today keeps a calm banner instead.
        advanceAfterPermission?.invoke()
    }
    val currentLanguage = Locale.getDefault().language
    OnboardingContent(
        permissions = permissions,
        manufacturer = Manufacturer.from(Build.MANUFACTURER),
        otherLanguageLabel = stringResource(
            if (currentLanguage == "tr") R.string.language_english else R.string.language_turkish
        ),
        onRequestNotifications = { next ->
            advanceAfterPermission = next
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                next()
            }
        },
        actions = OnboardingActions(
            onOpenExactAlarmSettings = { SystemSettings.openExactAlarmSettings(context) },
            onOpenBatterySettings = { SystemSettings.openBatteryOptimizationSettings(context) },
            onSwitchLanguage = if (AppLanguage.isSupported) {
                {
                    val target = if (currentLanguage == "tr") AppLanguage.ENGLISH else AppLanguage.TURKISH
                    AppLanguage.set(context, target)
                }
            } else {
                null
            },
            onFinish = { viewModel.finish(onFinished) }
        )
    )
}

@Composable
fun OnboardingContent(
    permissions: PermissionSnapshot,
    manufacturer: Manufacturer,
    otherLanguageLabel: String,
    onRequestNotifications: (next: () -> Unit) -> Unit,
    actions: OnboardingActions,
    showNotificationPage: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
) {
    // Below Android 13 notifications need no runtime permission, so that page is skipped entirely.
    val pages = buildList {
        add(OnboardingPage.WELCOME)
        if (showNotificationPage) add(OnboardingPage.NOTIFICATIONS)
        add(OnboardingPage.ON_TIME)
    }
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val goTo: (Int) -> Unit = { page -> scope.launch { pagerState.animateScrollToPage(page) } }
    val next = { goTo(pagerState.currentPage + 1) }
    val back = { goTo(pagerState.currentPage - 1) }
    BackHandler(enabled = pagerState.currentPage > 0) { back() }

    Surface(Modifier.fillMaxSize(), color = Doz.colors.bgApp) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            HorizontalPager(state = pagerState, userScrollEnabled = false, modifier = Modifier.weight(1f)) { index ->
                when (pages[index]) {
                    OnboardingPage.WELCOME -> WelcomePage(otherLanguageLabel, actions.onSwitchLanguage)
                    OnboardingPage.NOTIFICATIONS -> NotificationPage(onBack = back)
                    OnboardingPage.ON_TIME -> OnTimePage(permissions, manufacturer, actions, onBack = back)
                }
            }
            PageDots(pagerState, pages.size)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.s24, vertical = Space.s16),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s8)
            ) {
                when (pages[pagerState.currentPage]) {
                    OnboardingPage.WELCOME ->
                        PrimaryButton(stringResource(R.string.onboarding_start), next, Modifier.fillMaxWidth())
                    OnboardingPage.NOTIFICATIONS -> {
                        PrimaryButton(
                            stringResource(R.string.onboarding_allow_notifications),
                            onClick = { onRequestNotifications { next() } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextAction(stringResource(R.string.onboarding_not_now), onClick = next)
                    }
                    OnboardingPage.ON_TIME -> PrimaryButton(
                        stringResource(R.string.onboarding_add_first),
                        onClick = actions.onFinish,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(state: PagerState, count: Int) {
    val description = stringResource(R.string.onboarding_page, state.currentPage + 1, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.s8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.s8)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        repeat(count) { index ->
            val active = index == state.currentPage
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (active) 32.dp else 8.dp)
                    .background(if (active) Doz.colors.primary else Doz.colors.borderStrong, DozShapes.pill)
            )
        }
    }
}

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.s24),
        verticalArrangement = Arrangement.spacedBy(Space.s20),
        content = content
    )
}

@Composable
private fun IconTile(icon: ImageVector) {
    Box(
        Modifier
            .size(112.dp)
            .background(Doz.colors.primaryContainer, RoundedCornerShape(Radius.xl)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Doz.colors.onPrimaryContainer, modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun PageTitle(text: String) {
    Text(
        text,
        style = Doz.type.headline,
        color = Doz.colors.textPrimary,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun WelcomePage(otherLanguageLabel: String, onSwitchLanguage: (() -> Unit)?) {
    PageColumn {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.End) {
            if (onSwitchLanguage != null) TextAction(otherLanguageLabel, onClick = onSwitchLanguage)
        }
        IconTile(DozIcons.Capsule)
        PageTitle(stringResource(R.string.onboarding_welcome_title))
        Bullet(Icons.Rounded.Check, stringResource(R.string.onboarding_point_subscription))
        Bullet(Icons.Outlined.PersonOutline, stringResource(R.string.onboarding_point_account))
        Bullet(Icons.Outlined.Shield, stringResource(R.string.onboarding_point_device))
        Text(stringResource(R.string.disclaimer), style = Doz.type.caption, color = Doz.colors.textSecondary)
    }
}

@Composable
private fun Bullet(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s16)) {
        Box(
            Modifier
                .size(40.dp)
                .background(Doz.colors.surfaceContainerHigh, DozShapes.pill),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Doz.colors.textPrimary, modifier = Modifier.size(22.dp))
        }
        Text(text, style = Doz.type.bodyLarge, color = Doz.colors.textPrimary)
    }
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = Doz.colors.textPrimary
        )
    }
}

@Composable
private fun NotificationPage(onBack: () -> Unit) {
    PageColumn {
        BackRow(onBack)
        IconTile(Icons.Outlined.NotificationsNone)
        PageTitle(stringResource(R.string.onboarding_notif_title))
        Text(
            stringResource(R.string.onboarding_notif_body),
            style = Doz.type.bodyLarge,
            color = Doz.colors.textSecondary
        )
        SampleNotification()
    }
}

/** Shows what a reminder looks like before the system permission dialog. */
@Composable
private fun SampleNotification() {
    val c = Doz.colors
    val description = stringResource(R.string.onboarding_sample_a11y)
    Surface(
        shape = RoundedCornerShape(Radius.xl),
        color = c.surfaceDefault,
        border = BorderStroke(1.dp, c.borderDefault),
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Column(Modifier.padding(Space.s20), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            Text(stringResource(R.string.onboarding_sample_app), style = Doz.type.caption, color = c.textSecondary)
            Text(
                stringResource(R.string.onboarding_sample_title),
                style = Doz.type.titleMedium,
                color = c.textPrimary
            )
            Text(
                stringResource(R.string.onboarding_sample_text),
                style = Doz.type.bodyMedium,
                color = c.textSecondary
            )
            Spacer(Modifier.height(Space.s8))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s24)) {
                listOf(R.string.action_taken, R.string.action_snooze, R.string.action_skip).forEach {
                    Text(stringResource(it), style = Doz.type.labelLarge, color = c.primary)
                }
            }
        }
    }
}

@Composable
private fun OnTimePage(
    permissions: PermissionSnapshot,
    initialBrand: Manufacturer,
    actions: OnboardingActions,
    onBack: () -> Unit
) {
    val c = Doz.colors
    var brand by rememberSaveable { mutableStateOf(initialBrand) }
    PageColumn {
        BackRow(onBack)
        PageTitle(stringResource(R.string.onboarding_on_time_title))
        Text(
            stringResource(R.string.onboarding_on_time_body),
            style = Doz.type.bodyLarge,
            color = c.textSecondary
        )
        StepCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s12)
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = c.primary)
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.onboarding_step_exact),
                        style = Doz.type.titleMedium,
                        color = c.textPrimary
                    )
                    Text(
                        stringResource(
                            if (permissions.exactAlarmsAllowed) {
                                R.string.onboarding_status_granted
                            } else {
                                R.string.onboarding_status_denied
                            }
                        ),
                        style = Doz.type.bodyMedium,
                        color = c.textSecondary
                    )
                }
                if (permissions.exactAlarmsAllowed) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = c.primary)
                } else {
                    TonalButton(stringResource(R.string.onboarding_grant), onClick = actions.onOpenExactAlarmSettings)
                }
            }
        }
        StepCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s12)
            ) {
                Icon(Icons.Outlined.BatteryStd, contentDescription = null, tint = c.primary)
                Text(
                    stringResource(R.string.onboarding_step_battery),
                    style = Doz.type.titleMedium,
                    color = c.textPrimary
                )
            }
            BatterySteps(brand, onSelect = { brand = it }, modifier = Modifier.padding(top = Space.s12))
            TonalButton(
                stringResource(R.string.battery_open_settings_short),
                onClick = actions.onOpenBatterySettings,
                modifier = Modifier.padding(top = Space.s12)
            )
        }
    }
}

@Composable
private fun StepCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = DozShapes.card,
        color = Doz.colors.surfaceDefault,
        border = BorderStroke(1.dp, Doz.colors.borderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Space.s16), content = content)
    }
}

@DozPreviews
@Composable
private fun OnboardingPreview() {
    DozDefteriTheme {
        OnboardingContent(
            permissions = PermissionSnapshot(notificationsAllowed = false, exactAlarmsAllowed = false),
            manufacturer = Manufacturer.XIAOMI,
            otherLanguageLabel = "English",
            onRequestNotifications = {},
            actions = OnboardingActions(onSwitchLanguage = {}),
            showNotificationPage = true
        )
    }
}
