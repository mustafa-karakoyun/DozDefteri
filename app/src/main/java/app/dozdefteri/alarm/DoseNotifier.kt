package app.dozdefteri.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.dozdefteri.MainActivity
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.repository.LowStockNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and shows every notification of the app. Dose notifications use the dose event id as notification id;
 * stock and backup notifications use their own tags so ids never collide.
 */
@Singleton
class DoseNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : LowStockNotifier {

    private val manager = NotificationManagerCompat.from(context)

    /** Creates the notification channels; safe to call repeatedly. */
    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        system.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_DOSE,
                    context.getString(R.string.channel_dose_reminders),
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_STOCK,
                    context.getString(R.string.channel_stock_alerts),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    CHANNEL_BACKUP,
                    context.getString(R.string.channel_backup_reminders),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        )
    }

    /** True when notifications can be posted (Android 13+ runtime permission and the app-level switch). */
    fun canPostNotifications(): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permitted && manager.areNotificationsEnabled()
    }

    /** Shows the reminder for one dose with Taken / Snooze / Skip actions. Silently does nothing without permission. */
    fun showDose(details: DoseEventDetails, showProfileName: Boolean) {
        if (!canPostNotifications()) return
        val event = details.event
        val time = Instant.ofEpochMilli(event.scheduledAt).atZone(ZoneId.systemDefault()).toLocalTime()
        val body = buildString {
            if (showProfileName) append(details.profileName).append(": ")
            append(Formats.dose(details.doseAmount, details.doseUnit))
            append(" · ")
            append(Formats.time(time, DateFormat.is24HourFormat(context)))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_DOSE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(details.medicationName)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(false)
            .setWhen(event.scheduledAt)
            .setShowWhen(true)
            .setGroup(GROUP_DOSE)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_TODAY))
            .addAction(0, context.getString(R.string.action_taken), actionIntent(event.id, ACTION_TAKE, 0))
            .addAction(0, context.getString(R.string.action_snooze), actionIntent(event.id, ACTION_SNOOZE, 1))
            .addAction(0, context.getString(R.string.action_skip), actionIntent(event.id, ACTION_SKIP, 2))
            .build()
        post(null, event.id.toInt(), notification)
        updateSummary()
    }

    fun cancelDose(eventId: Long) {
        manager.cancel(eventId.toInt())
        updateSummary()
    }

    fun cancelAll() {
        manager.cancelAll()
    }

    override fun notifyLowStock(medication: MedicationEntity) {
        if (!canPostNotifications()) return
        val stock = medication.stockQuantity ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_STOCK)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.low_stock_notification_title, medication.name))
            .setContentText(
                context.getString(
                    R.string.low_stock_notification_text,
                    Formats.dose(stock, medication.doseUnit)
                )
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_TODAY))
            .build()
        post(TAG_STOCK, medication.id.toInt(), notification)
    }

    fun showBackupReminder() {
        if (!canPostNotifications()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.backup_reminder_title))
            .setContentText(context.getString(R.string.backup_reminder_text))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_EXPORT))
            .build()
        post(TAG_BACKUP, BACKUP_NOTIFICATION_ID, notification)
    }

    /** Posts or removes the group summary depending on how many dose notifications are visible. */
    private fun updateSummary() {
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        val visibleDoses = system.activeNotifications.count {
            it.tag == null && it.id != SUMMARY_ID && it.notification.group == GROUP_DOSE
        }
        if (visibleDoses < 2) {
            manager.cancel(SUMMARY_ID)
            return
        }
        if (!canPostNotifications()) return
        val summary = NotificationCompat.Builder(context, CHANNEL_DOSE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.resources.getQuantityString(R.plurals.dose_summary_title, visibleDoses, visibleDoses))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_DOSE)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_TODAY))
            .build()
        post(null, SUMMARY_ID, summary)
    }

    // Callers check canPostNotifications() first; the permission can still be revoked in between.
    @SuppressLint("MissingPermission")
    private fun post(tag: String?, id: Int, notification: android.app.Notification) {
        if (!canPostNotifications()) return
        try {
            manager.notify(tag, id, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently; nothing to show.
        }
    }

    private fun openAppIntent(destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DESTINATION, destination)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            destination.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(eventId: Long, action: String, actionIndex: Int): PendingIntent {
        val intent = Intent(context, DoseActionReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_EVENT_ID, eventId)
        return PendingIntent.getBroadcast(
            context,
            (eventId * 10 + actionIndex).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_DOSE = "dose_reminders"
        const val CHANNEL_STOCK = "stock_alerts"
        const val CHANNEL_BACKUP = "backup_reminders"
        const val GROUP_DOSE = "dose_group"
        const val SUMMARY_ID = 0
        const val TAG_STOCK = "stock"
        const val TAG_BACKUP = "backup"
        const val BACKUP_NOTIFICATION_ID = 1
        const val ACTION_TAKE = "app.dozdefteri.action.TAKE"
        const val ACTION_SNOOZE = "app.dozdefteri.action.SNOOZE"
        const val ACTION_SKIP = "app.dozdefteri.action.SKIP"
        const val EXTRA_EVENT_ID = "event_id"
    }
}
