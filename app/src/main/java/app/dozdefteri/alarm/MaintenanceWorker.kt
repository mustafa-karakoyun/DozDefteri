package app.dozdefteri.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.dozdefteri.data.prefs.SettingsRepository
import app.dozdefteri.data.repository.MedicationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Periodic safety net (every 6 hours): re-runs [DoseSync] in case an alarm was lost and shows a backup reminder
 * when 30 days passed since the previous one and at least one medication exists.
 */
@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val doseSync: DoseSync,
    private val settingsRepository: SettingsRepository,
    private val medicationRepository: MedicationRepository,
    private val notifier: DoseNotifier,
    private val clock: Clock
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        doseSync.run("worker")
        maybeRemindBackup()
        return Result.success()
    }

    private suspend fun maybeRemindBackup() {
        val now = clock.millis()
        val settings = settingsRepository.current()
        if (!settings.backupReminderEnabled) return
        val last = settings.lastBackupReminderAt
        if (last == 0L) {
            // First run after install: start the 30-day period instead of reminding immediately.
            settingsRepository.setLastBackupReminderAt(now)
            return
        }
        if (now - last >= BACKUP_REMINDER_INTERVAL.toMillis() && medicationRepository.count() > 0) {
            notifier.showBackupReminder()
            settingsRepository.setLastBackupReminderAt(now)
        }
    }

    companion object {
        const val UNIQUE_NAME = "dose_maintenance"
        private val BACKUP_REMINDER_INTERVAL: Duration = Duration.ofDays(30)

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(6, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
