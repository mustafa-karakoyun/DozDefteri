package app.dozdefteri.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.dozdefteri.data.repository.DoseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps exactly one alarm armed: the earliest open dose that has not been notified yet.
 * Every call replaces the previous alarm because the [PendingIntent] identity never changes.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val doseRepository: DoseRepository,
    private val clock: Clock
) {
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    /** Exact alarms are always allowed below Android 12; on 12+ the user may revoke the permission. */
    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    /**
     * Arms the alarm for the next PENDING/SNOOZED dose with `notifiedAt == null`, at
     * `max(scheduledAt, now + 1 s)`. Cancels the alarm when there is no such dose. Uses an exact alarm when allowed,
     * otherwise an inexact `setAndAllowWhileIdle` (the UI shows a warning in that case).
     */
    suspend fun scheduleNext() {
        val manager = alarmManager ?: return
        val pendingIntent = alarmPendingIntent()
        val next = doseRepository.nextUnnotifiedTime()
        if (next == null) {
            manager.cancel(pendingIntent)
            return
        }
        val trigger = maxOf(next, clock.millis() + MIN_DELAY_MILLIS)
        if (canScheduleExactAlarms()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
                return
            } catch (_: SecurityException) {
                // Permission revoked between the check and the call; fall back to an inexact alarm.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(context, DoseAlarmReceiver::class.java).setAction(ACTION_DOSE_ALARM)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val REQUEST_CODE = 1001
        const val ACTION_DOSE_ALARM = "app.dozdefteri.action.DOSE_ALARM"
        private const val MIN_DELAY_MILLIS = 1_000L
    }
}
