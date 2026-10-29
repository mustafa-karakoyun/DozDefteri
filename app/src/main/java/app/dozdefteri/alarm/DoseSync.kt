package app.dozdefteri.alarm

import android.util.Log
import app.dozdefteri.data.prefs.SettingsRepository
import app.dozdefteri.data.repository.DoseRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single entry point that brings dose events, notifications and the alarm into a consistent state.
 * Called on app resume, after every medication/schedule/profile change, from all receivers and the worker.
 */
@Singleton
class DoseSync @Inject constructor(
    private val doseRepository: DoseRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val notifier: DoseNotifier,
    private val widgetUpdater: WidgetUpdater,
    private val clock: Clock
) {
    private val mutex = Mutex()

    /**
     * Runs, in this exact order: mark overdue doses MISSED, fill the 48-hour window, cancel the notifications of the
     * doses that were just missed, arm the next alarm, refresh widgets. Concurrent calls are serialized.
     */
    suspend fun run(reason: String) {
        mutex.withLock {
            Log.d(TAG, "sync: $reason")
            val now = clock.instant()
            val missedAfter = settingsRepository.current().missedAfterMinutes
            val missedIds = doseRepository.markMissed(now, missedAfter)
            doseRepository.ensureWindow(now)
            missedIds.forEach(notifier::cancelDose)
            alarmScheduler.scheduleNext()
        }
        widgetUpdater.updateAll()
    }

    private companion object {
        const val TAG = "DoseSync"
    }
}
