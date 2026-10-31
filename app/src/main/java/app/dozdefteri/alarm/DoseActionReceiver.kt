package app.dozdefteri.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.domain.StockEstimator
import java.time.Duration

/**
 * Handles the notification buttons without opening the app: Aldım / Hepsini aldım, Ertele, Atla and Geri al.
 * Taking or snoozing replaces the reminder with a short confirmation (plan UI section 4, "0b").
 */
class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ids = intent.getLongArrayExtra(DoseNotifier.EXTRA_EVENT_IDS)?.takeIf { it.isNotEmpty() } ?: return
        val action = intent.action ?: return
        val entryPoint = ReceiverEntryPoint.get(context)
        runAsync(TAG) {
            val clock = entryPoint.clock()
            val now = clock.instant()
            val repository = entryPoint.doseRepository()
            val notifier = entryPoint.notifier()
            // Every call is a no-op for doses that are no longer open, so repeated taps are safe.
            when (action) {
                DoseNotifier.ACTION_TAKE -> {
                    val taken = ids.filter { repository.take(it, now) }
                    notifier.cancelReminder(ids)
                    if (taken.isNotEmpty()) {
                        val (title, stockLine, fraction) = confirmation(context, entryPoint, taken)
                        notifier.showTakenConfirmation(
                            taken.toLongArray(),
                            title,
                            now.atZone(clock.zone).toLocalTime(),
                            stockLine,
                            fraction
                        )
                    }
                }
                DoseNotifier.ACTION_SNOOZE -> {
                    val minutes = entryPoint.settingsRepository().current().snoozeMinutes
                    val snoozed = ids.filter { repository.snooze(it, now, minutes) }
                    notifier.cancelReminder(ids)
                    if (snoozed.isNotEmpty()) {
                        val until = now.plus(Duration.ofMinutes(minutes.toLong())).atZone(clock.zone).toLocalTime()
                        notifier.showSnoozedConfirmation(
                            snoozed.toLongArray(),
                            title(context, entryPoint, snoozed),
                            until
                        )
                    }
                }
                DoseNotifier.ACTION_SKIP -> {
                    ids.forEach { repository.skip(it, now) }
                    notifier.cancelReminder(ids)
                }
                DoseNotifier.ACTION_UNDO -> {
                    ids.forEach { repository.undo(it, now) }
                    notifier.cancelReminder(ids)
                }
            }
            entryPoint.doseSync().run("action")
        }
    }

    /** "Metformin" for one dose, "2 ilaç" for several. */
    private suspend fun title(context: Context, entryPoint: ReceiverEntryPoint, ids: List<Long>): String {
        if (ids.size > 1) {
            return context.resources.getQuantityString(R.plurals.notif_medication_count, ids.size, ids.size)
        }
        return entryPoint.doseRepository().getDetails(ids.single())?.medicationName.orEmpty()
    }

    private data class Confirmation(val title: String, val stockLine: String?, val fraction: Float?)

    /** Title plus, for a single tracked medication, "Kalan: 37 tablet · ~18 gün" and the stock bar. */
    private suspend fun confirmation(
        context: Context,
        entryPoint: ReceiverEntryPoint,
        ids: List<Long>
    ): Confirmation {
        val title = title(context, entryPoint, ids)
        if (ids.size > 1) return Confirmation(title, null, null)
        val details = entryPoint.doseRepository().getDetails(ids.single()) ?: return Confirmation(title, null, null)
        val loaded = entryPoint.medicationRepository().get(details.event.medicationId)
        val medication = loaded?.medication ?: return Confirmation(title, null, null)
        val stock = medication.stockQuantity ?: return Confirmation(title, null, null)
        val days = StockEstimator.remainingDays(stock, listOfNotNull(loaded.schedule), medication.doseAmount)
        val amount = Formats.dose(stock, medication.doseUnit)
        val line = if (days != null) {
            context.getString(R.string.notif_remaining_days, amount, days)
        } else {
            context.getString(R.string.notif_remaining, amount)
        }
        val fraction = ((days?.toFloat() ?: stock.toFloat()) / FULL_STOCK_DAYS).coerceIn(0f, 1f)
        return Confirmation(title, line, fraction)
    }

    private companion object {
        const val TAG = "DoseActionReceiver"
        const val FULL_STOCK_DAYS = 30f
    }
}
