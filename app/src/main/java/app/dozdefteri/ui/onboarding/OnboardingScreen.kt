package app.dozdefteri.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.dozdefteri.R
import app.dozdefteri.ui.components.PermissionSnapshot
import app.dozdefteri.ui.components.SystemSettings
import app.dozdefteri.ui.components.rememberPermissionSnapshot
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.StatusTaken
import kotlinx.coroutines.launch

private enum class OnboardingPage { VALUE, NOTIFICATIONS, EXACT_ALARM }

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onOpenBatteryGuide: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val permissions = rememberPermissionSnapshot()
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationAsked = true
    }
    OnboardingContent(
        permissions = permissions,
        notificationAsked = notificationAsked,
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onOpenNotificationSettings = { SystemSettings.openNotificationSettings(context) },
        onOpenExactAlarmSettings = { SystemSettings.openExactAlarmSettings(context) },
        onOpenBatteryGuide = onOpenBatteryGuide,
        onStart = { viewModel.finish(onFinished) }
    )
}

@Composable
fun OnboardingContent(
    permissions: PermissionSnapshot,
    notificationAsked: Boolean,
    onRequestNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatteryGuide: () -> Unit,
    onStart: () -> Unit,
    showNotificationPage: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
) {
    // Below Android 13 notifications need no runtime permission, so that page is skipped entirely.
    val pages = buildList {
        add(OnboardingPage.VALUE)
        if (showNotificationPage) add(OnboardingPage.NOTIFICATIONS)
        add(OnboardingPage.EXACT_ALARM)
    }
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val next: () -> Unit = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }

    Surface(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
        ) { index ->
            when (pages[index]) {
                OnboardingPage.VALUE -> ValuePage(onContinue = next)
                OnboardingPage.NOTIFICATIONS -> NotificationPage(
                    granted = permissions.notificationsAllowed,
                    asked = notificationAsked,
                    onRequest = onRequestNotifications,
                    onOpenSettings = onOpenNotificationSettings,
                    onContinue = next
                )
                OnboardingPage.EXACT_ALARM -> ExactAlarmPage(
                    granted = permissions.exactAlarmsAllowed,
                    onOpenSettings = onOpenExactAlarmSettings,
                    onOpenBatteryGuide = onOpenBatteryGuide,
                    onStart = onStart
                )
            }
        }
    }
}

@Composable
private fun PageLayout(
    icon: ImageVector,
    title: String,
    body: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.heightIn(min = 24.dp))
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun ValuePage(onContinue: () -> Unit) {
    PageLayout(
        icon = Icons.Filled.CheckCircle,
        title = stringResource(R.string.onboarding_value_title),
        body = stringResource(R.string.onboarding_value_body)
    ) {
        Bullet(Icons.Filled.MoneyOff, stringResource(R.string.onboarding_point_no_subscription))
        Bullet(Icons.Filled.NoAccounts, stringResource(R.string.onboarding_point_no_account))
        Bullet(Icons.Filled.PhoneAndroid, stringResource(R.string.onboarding_point_on_device))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.onboarding_continue))
        }
        Text(
            stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationPage(
    granted: Boolean,
    asked: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit
) {
    PageLayout(
        icon = Icons.Filled.Notifications,
        title = stringResource(R.string.onboarding_notifications_title),
        body = stringResource(R.string.onboarding_notifications_body)
    ) {
        when {
            granted -> {
                GrantedRow(stringResource(R.string.onboarding_permission_granted))
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.onboarding_continue))
                }
            }
            asked -> {
                Text(stringResource(R.string.onboarding_notifications_denied))
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.open_settings))
                }
                TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.onboarding_skip_for_now))
                }
            }
            else -> {
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.onboarding_allow))
                }
                TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.onboarding_skip_for_now))
                }
            }
        }
    }
}

@Composable
private fun ExactAlarmPage(
    granted: Boolean,
    onOpenSettings: () -> Unit,
    onOpenBatteryGuide: () -> Unit,
    onStart: () -> Unit
) {
    PageLayout(
        icon = Icons.Filled.AccessAlarm,
        title = stringResource(R.string.onboarding_exact_title),
        body = stringResource(R.string.onboarding_exact_body)
    ) {
        if (granted) {
            GrantedRow(stringResource(R.string.onboarding_permission_granted))
        } else {
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.open_settings))
            }
        }
        TextButton(onClick = onOpenBatteryGuide, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.battery_guide_link))
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.onboarding_start))
        }
    }
}

@Composable
private fun Bullet(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GrantedRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusTaken)
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun OnboardingPreview() {
    DozDefteriTheme {
        OnboardingContent(
            permissions = PermissionSnapshot(notificationsAllowed = false, exactAlarmsAllowed = false),
            notificationAsked = false,
            onRequestNotifications = {},
            onOpenNotificationSettings = {},
            onOpenExactAlarmSettings = {},
            onOpenBatteryGuide = {},
            onStart = {},
            showNotificationPage = true
        )
    }
}
