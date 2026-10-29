package app.dozdefteri.alarm

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import app.dozdefteri.widget.NextDoseWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Refreshes home-screen widgets after data changes. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun updateAll() {
        try {
            NextDoseWidget().updateAll(context)
        } catch (e: IllegalStateException) {
            Log.w("WidgetUpdater", "Widget update failed", e)
        }
    }
}
