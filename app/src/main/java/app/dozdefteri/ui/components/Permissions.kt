package app.dozdefteri.ui.components

import android.Manifest
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Permission state that is re-read every time the screen resumes (e.g. after returning from system settings). */
data class PermissionSnapshot(
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean
)

fun readPermissions(context: Context): PermissionSnapshot {
    val notificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val notifications = notificationPermission && NotificationManagerCompat.from(context).areNotificationsEnabled()
    val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    return PermissionSnapshot(notificationsAllowed = notifications, exactAlarmsAllowed = exact)
}

@Composable
fun rememberPermissionSnapshot(): PermissionSnapshot {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(readPermissions(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        snapshot = readPermissions(context)
    }
    return snapshot
}

/** System screens the app links to. Every launcher falls back to the app details page if the target is missing. */
object SystemSettings {
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launch(
                context,
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            )
        } else {
            openAppDetails(context)
        }
    }

    fun openNotificationSettings(context: Context) {
        launch(
            context,
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }

    fun openBatteryOptimizationSettings(context: Context) {
        launch(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    fun openAppDetails(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // No settings app available; nothing else to try.
        }
    }

    private fun launch(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            openAppDetails(context)
        }
    }
}
