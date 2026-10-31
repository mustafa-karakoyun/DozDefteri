package app.dozdefteri.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.dozdefteri.MainActivity
import app.dozdefteri.R
import app.dozdefteri.core.format.DoseText
import app.dozdefteri.core.format.Formats
import app.dozdefteri.core.format.TurkishSuffix
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.repository.LowStockNotifier
import app.dozdefteri.ui.theme.Teal
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and shows every notification (plan UI section 4). Doses due in the same minute share one notification
 * (tag [TAG_GROUP], id = smallest event id); a single dose uses its event id without a tag. Stock and backup
 * notifications have their own tags so ids never collide. Only standard templates are used so large font sizes
 * wrap instead of clipping.
 */
@Singleton
class DoseNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : LowStockNotifier {

    private val manager = NotificationManagerCompat.from(context)
    private val accent = Teal.c600.toArgb()

    /** Creates the notification channels; safe to call repeatedly. */
    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        // Stock alerts moved to a quiet channel; importance cannot be lowered on an existing channel.
        system.deleteNotificationChannel(LEGACY_CHANNEL_STOCK)
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
                    NotificationManager.IMPORTANCE_LOW
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

    /**
     * Shows the reminder for doses due at the same minute: one dose gets Aldım · Ertele · Atla, several doses get
     * one notification with Hepsini aldım · Ertele · Aç. Does nothing without notification permission.
     */
    fun showDoses(doses: List<DoseEventDetails>, showProfileName: Boolean, snoozeMinutes: Int) {
        if (doses.isEmpty() || !canPostNotifications()) return
        val items = doses.sortedBy { it.medicationName.lowercase() }.map { NotifiedDose.from(it, showProfileName) }
        if (items.size == 1) postSingle(items.single(), snoozeMinutes) else postGroup(items, snoozeMinutes)
    }

    /** Removes the reminder that showed exactly [eventIds] (the notification whose button was pressed). */
    fun cancelReminder(eventIds: LongArray) {
        if (eventIds.size == 1) {
            manager.cancel(eventIds.single().toInt())
        } else {
            manager.cancel(TAG_GROUP, groupId(eventIds))
        }
    }

    private fun postSingle(dose: NotifiedDose, snoozeMinutes: Int, silent: Boolean = false) {
        val time = formatTime(dose.time)
        val text = context.getString(R.string.notif_dose_text, time, dose.detail)
        val notification = baseDose(silent)
            .setContentTitle(dose.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setWhen(dose.scheduledAt)
            .setShowWhen(true)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_TODAY))
            .addExtras(Extras.single(dose).apply { putInt(EXTRA_SNOOZE_MINUTES, snoozeMinutes) })
            .addAction(
                0,
                context.getString(R.string.action_taken),
                actionIntent(ACTION_TAKE, longArrayOf(dose.id), 0)
            )
            .addAction(
                0,
                context.getString(R.string.notif_action_snooze, snoozeMinutes),
                actionIntent(ACTION_SNOOZE, longArrayOf(dose.id), 1)
            )
            .addAction(
                0,
                context.getString(R.string.action_skip),
                actionIntent(ACTION_SKIP, longArrayOf(dose.id), 2)
            )
            .build()
        post(null, dose.id.toInt(), notification)
    }

    private fun postGroup(doses: List<NotifiedDose>, snoozeMinutes: Int, silent: Boolean = false) {
        val ids = doses.map { it.id }.toLongArray()
        val time = formatTime(doses.first().time)
        val title = context.resources.getQuantityString(R.plurals.notif_group_title, doses.size, time, doses.size)
        val inbox = NotificationCompat.InboxStyle().setSummaryText(context.getString(R.string.notif_group_hint))
        doses.forEach { inbox.addLine(it.line) }
        val notification = baseDose(silent)
            .setContentTitle(title)
            .setContentText(doses.joinToString(", ") { it.name })
            .setStyle(inbox)
            .setWhen(doses.first().scheduledAt)
            .setShowWhen(true)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_TODAY))
            .addExtras(Extras.group(doses).apply { putInt(EXTRA_SNOOZE_MINUTES, snoozeMinutes) })
            .addAction(0, context.getString(R.string.notif_action_take_all), actionIntent(ACTION_TAKE, ids, 0))
            .addAction(
                0,
                context.getString(R.string.notif_action_snooze, snoozeMinutes),
                actionIntent(ACTION_SNOOZE, ids, 1)
            )
            .addAction(0, context.getString(R.string.notif_action_open), openAppIntent(MainActivity.DESTINATION_TODAY))
            .build()
        post(TAG_GROUP, groupId(ids), notification)
    }

    /**
     * Replaces the reminder with a short "✓ Metformin alındı · 20:02" confirmation that disappears after
     * 4 seconds.
     * [stockLine] is e.g. "Kalan: 37 tablet · ~18 gün"; [stockFraction] (0..1) draws a progress bar.
     */
    fun showTakenConfirmation(
        eventIds: LongArray,
        title: String,
        takenAt: LocalTime,
        stockLine: String?,
        stockFraction: Float?
    ) {
        if (!canPostNotifications()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_DOSE)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setContentTitle(context.getString(R.string.notif_taken_title, title, formatTime(takenAt)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(CONFIRMATION_TIMEOUT_MILLIS)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_TODAY))
            .addAction(0, context.getString(R.string.undo), actionIntent(ACTION_UNDO, eventIds, 3))
        if (stockLine != null) builder.setContentText(stockLine)
        if (stockFraction != null) builder.setProgress(PROGRESS_MAX, (stockFraction * PROGRESS_MAX).toInt(), false)
        postReplacing(eventIds, builder.build())
    }

    /** "⏱ Metformin 20:10'a ertelendi" / "Tekrar hatırlatacağız." with "Şimdi aldım". */
    fun showSnoozedConfirmation(eventIds: LongArray, title: String, until: LocalTime) {
        if (!canPostNotifications()) return
        val formatted = formatTime(until)
        val timeText = if (TurkishSuffix.isTurkish()) TurkishSuffix.timeDative(formatted, until) else formatted
        val notification = NotificationCompat.Builder(context, CHANNEL_DOSE)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setContentTitle(context.getString(R.string.notif_snoozed_title, title, timeText))
            .setContentText(context.getString(R.string.notif_snoozed_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_TODAY))
            .addAction(0, context.getString(R.string.take_now), actionIntent(ACTION_TAKE, eventIds, 4))
            .build()
        postReplacing(eventIds, notification)
    }

    /** Removes the dose from whatever notification shows it; a group notification keeps its remaining doses. */
    fun cancelDose(eventId: Long) {
        manager.cancel(eventId.toInt())
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        system.activeNotifications
            .filter { active ->
                active.tag == TAG_GROUP &&
                    eventId in (active.notification.extras.getLongArray(EXTRA_EVENT_IDS) ?: LongArray(0))
            }
            .forEach { active ->
                val remaining = Extras.read(active.notification.extras).filter { it.id != eventId }
                manager.cancel(TAG_GROUP, active.id)
                val snooze = active.notification.extras.getInt(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE)
                when {
                    // Rebuilt reminders stay silent: the user was already alerted for them.
                    remaining.size == 1 -> postSingle(remaining.single(), snooze, silent = true)
                    remaining.size > 1 -> postGroup(remaining, snooze, silent = true)
                }
            }
    }

    fun cancelAll() {
        manager.cancelAll()
    }

    override fun notifyLowStock(medication: MedicationEntity, remainingDays: Int?) {
        if (!canPostNotifications()) return
        val stock = medication.stockQuantity ?: return
        val amount = Formats.dose(stock, medication.doseUnit)
        val text = if (remainingDays != null) {
            context.getString(R.string.notif_stock_text_days, amount, remainingDays)
        } else {
            context.getString(R.string.notif_stock_text, amount)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_STOCK)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setContentTitle(context.getString(R.string.low_stock_title, medication.name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_TODAY))
            .addAction(
                0,
                context.getString(R.string.add_stock),
                openAppIntent(MainActivity.DESTINATION_STOCK_PREFIX + medication.id)
            )
            .build()
        post(TAG_STOCK, medication.id.toInt(), notification)
    }

    fun showBackupReminder() {
        if (!canPostNotifications()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setContentTitle(context.getString(R.string.backup_reminder_title))
            .setContentText(context.getString(R.string.backup_reminder_text))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(MainActivity.DESTINATION_EXPORT))
            .build()
        post(TAG_BACKUP, BACKUP_NOTIFICATION_ID, notification)
    }

    private fun baseDose(silent: Boolean): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_DOSE)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(false)
            .setSilent(silent)

    /** Posts [notification] in place of the single or group reminder that showed [eventIds]. */
    private fun postReplacing(eventIds: LongArray, notification: Notification) {
        if (eventIds.size == 1) {
            post(null, eventIds.single().toInt(), notification)
        } else {
            post(TAG_GROUP, groupId(eventIds), notification)
        }
    }

    // Callers check canPostNotifications() first; the permission can still be revoked in between.
    @SuppressLint("MissingPermission")
    private fun post(tag: String?, id: Int, notification: Notification) {
        if (!canPostNotifications()) return
        try {
            manager.notify(tag, id, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently; nothing to show.
        }
    }

    private fun formatTime(time: LocalTime): String = Formats.time(time, DateFormat.is24HourFormat(context))

    private fun openAppIntent(destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DESTINATION, destination)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        return PendingIntent.getActivity(
            context,
            destination.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(action: String, eventIds: LongArray, actionIndex: Int): PendingIntent {
        val intent = Intent(context, DoseActionReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_EVENT_IDS, eventIds)
        return PendingIntent.getBroadcast(
            context,
            (groupId(eventIds).toLong() * 10 + actionIndex).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Snapshot of a dose shown in a notification; kept in the extras so a group can be rebuilt without the DB. */
    private data class NotifiedDose(
        val id: Long,
        val name: String,
        val title: String,
        val detail: String,
        val line: String,
        val scheduledAt: Long
    ) {
        val time: LocalTime get() = Instant.ofEpochMilli(scheduledAt).atZone(ZoneId.systemDefault()).toLocalTime()

        companion object {
            fun from(details: DoseEventDetails, showProfileName: Boolean): NotifiedDose {
                val strength = DoseText.strength(details.strengthAmount, details.strengthUnit)
                val amount = Formats.dose(details.event.quantity, details.doseUnit)
                val note = details.medicationNote?.trim()?.ifEmpty { null }
                val prefix = if (showProfileName) "${details.profileName}: " else ""
                return NotifiedDose(
                    id = details.event.id,
                    name = details.medicationName,
                    title = prefix + DoseText.title(details.medicationName, strength),
                    detail = listOfNotNull(amount, note).joinToString(", "),
                    line = DoseText.title(details.medicationName, strength) + DoseText.SEPARATOR + amount,
                    scheduledAt = details.event.scheduledAt
                )
            }
        }
    }

    private object Extras {
        fun single(dose: NotifiedDose) = group(listOf(dose))

        fun group(doses: List<NotifiedDose>) = Bundle().apply {
            putLongArray(EXTRA_EVENT_IDS, doses.map { it.id }.toLongArray())
            putStringArray(EXTRA_NAMES, doses.map { it.name }.toTypedArray())
            putStringArray(EXTRA_TITLES, doses.map { it.title }.toTypedArray())
            putStringArray(EXTRA_DETAILS, doses.map { it.detail }.toTypedArray())
            putStringArray(EXTRA_LINES, doses.map { it.line }.toTypedArray())
            putLongArray(EXTRA_TIMES, doses.map { it.scheduledAt }.toLongArray())
        }

        fun read(extras: Bundle): List<NotifiedDose> {
            val ids = extras.getLongArray(EXTRA_EVENT_IDS) ?: return emptyList()
            val names = extras.getStringArray(EXTRA_NAMES) ?: return emptyList()
            val titles = extras.getStringArray(EXTRA_TITLES) ?: return emptyList()
            val details = extras.getStringArray(EXTRA_DETAILS) ?: return emptyList()
            val lines = extras.getStringArray(EXTRA_LINES) ?: return emptyList()
            val times = extras.getLongArray(EXTRA_TIMES) ?: return emptyList()
            return ids.indices.map { NotifiedDose(ids[it], names[it], titles[it], details[it], lines[it], times[it]) }
        }
    }

    companion object {
        const val CHANNEL_DOSE = "dose_reminders"
        const val CHANNEL_STOCK = "stock_alerts_quiet"
        const val CHANNEL_BACKUP = "backup_reminders"
        private const val LEGACY_CHANNEL_STOCK = "stock_alerts"
        const val TAG_GROUP = "dose_group"
        const val TAG_STOCK = "stock"
        const val TAG_BACKUP = "backup"
        const val BACKUP_NOTIFICATION_ID = 1
        const val ACTION_TAKE = "app.dozdefteri.action.TAKE"
        const val ACTION_SNOOZE = "app.dozdefteri.action.SNOOZE"
        const val ACTION_SKIP = "app.dozdefteri.action.SKIP"
        const val ACTION_UNDO = "app.dozdefteri.action.UNDO"
        const val EXTRA_EVENT_IDS = "event_ids"
        private const val EXTRA_NAMES = "dose_names"
        private const val EXTRA_TITLES = "dose_titles"
        private const val EXTRA_DETAILS = "dose_details"
        private const val EXTRA_LINES = "dose_lines"
        private const val EXTRA_TIMES = "dose_times"
        private const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        private const val DEFAULT_SNOOZE = 10
        private const val CONFIRMATION_TIMEOUT_MILLIS = 4_000L
        private const val PROGRESS_MAX = 100

        fun groupId(eventIds: LongArray): Int = (eventIds.minOrNull() ?: 0L).toInt()
    }
}
