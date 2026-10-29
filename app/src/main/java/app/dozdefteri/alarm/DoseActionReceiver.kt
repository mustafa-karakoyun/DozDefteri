package app.dozdefteri.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the Taken / Snooze / Skip buttons of a dose notification. */
class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(DoseNotifier.EXTRA_EVENT_ID, -1L)
        if (eventId < 0) return
        val entryPoint = ReceiverEntryPoint.get(context)
        runAsync(TAG) {
            val now = entryPoint.clock().instant()
            val repository = entryPoint.doseRepository()
            // Each call is a no-op when the dose is no longer PENDING/SNOOZED; the notification is cancelled anyway.
            when (intent.action) {
                DoseNotifier.ACTION_TAKE -> repository.take(eventId, now)
                DoseNotifier.ACTION_SNOOZE -> {
                    val minutes = entryPoint.settingsRepository().current().snoozeMinutes
                    repository.snooze(eventId, now, minutes)
                }
                DoseNotifier.ACTION_SKIP -> repository.skip(eventId, now)
            }
            entryPoint.notifier().cancelDose(eventId)
            entryPoint.doseSync().run("action")
        }
    }

    private companion object {
        const val TAG = "DoseActionReceiver"
    }
}
