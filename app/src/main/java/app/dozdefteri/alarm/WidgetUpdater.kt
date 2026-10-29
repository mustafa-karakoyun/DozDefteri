package app.dozdefteri.alarm

import javax.inject.Inject
import javax.inject.Singleton

/** Refreshes home-screen widgets after data changes. */
@Singleton
class WidgetUpdater @Inject constructor() {
    suspend fun updateAll() {
        // Widgets are added in phase 8.
    }
}
