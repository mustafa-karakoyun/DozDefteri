package app.dozdefteri.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fired by [AlarmScheduler]. Shows every open, not yet notified dose due within the next 30 seconds — doses of the
 * same minute together in one notification — marks them notified and re-runs [DoseSync] to arm the next alarm.
 */
class DoseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val entryPoint = ReceiverEntryPoint.get(context)
        runAsync(TAG) {
            val now = entryPoint.clock().instant()
            val repository = entryPoint.doseRepository()
            val due = repository.dueUnnotified(now.plusSeconds(DUE_TOLERANCE_SECONDS))
            if (due.isNotEmpty()) {
                val showProfileName = entryPoint.profileRepository().count() > 1
                val snooze = entryPoint.settingsRepository().current().snoozeMinutes
                due.groupBy { it.event.scheduledAt / MINUTE_MILLIS }.values.forEach { sameMinute ->
                    entryPoint.notifier().showDoses(sameMinute, showProfileName, snooze)
                }
                repository.markNotified(due.map { it.event.id }, now)
            }
            entryPoint.doseSync().run("alarm")
        }
    }

    private companion object {
        const val TAG = "DoseAlarmReceiver"
        const val DUE_TOLERANCE_SECONDS = 30L
        const val MINUTE_MILLIS = 60_000L
    }
}
