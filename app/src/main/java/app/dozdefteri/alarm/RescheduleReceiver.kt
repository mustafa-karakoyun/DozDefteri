package app.dozdefteri.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the alarm after boot, clock or time-zone changes, app updates and exact-alarm permission changes.
 * LOCKED_BOOT_COMPLETED is declared too, but as the receiver is not direct-boot aware the database is only
 * reachable after unlock; any failure is caught and BOOT_COMPLETED does the work.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return
        val entryPoint = ReceiverEntryPoint.get(context)
        runAsync(TAG) {
            entryPoint.doseSync().run("reschedule:$action")
        }
    }

    private companion object {
        const val TAG = "RescheduleReceiver"
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
    }
}
