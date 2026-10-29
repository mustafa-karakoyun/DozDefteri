package app.dozdefteri.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.dozdefteri.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val onboardingDone: Boolean = false,
    /** null until the first profile has been created. */
    val activeProfileId: Long? = null,
    val snoozeMinutes: Int = SettingsRepository.DEFAULT_SNOOZE_MINUTES,
    val missedAfterMinutes: Int = SettingsRepository.DEFAULT_MISSED_AFTER_MINUTES,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentIndex: Int = 0,
    val proCached: Boolean = false,
    val lastBackupReminderAt: Long = 0L
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            onboardingDone = prefs[ONBOARDING_DONE] ?: false,
            activeProfileId = prefs[ACTIVE_PROFILE_ID],
            snoozeMinutes = (prefs[SNOOZE_MINUTES] ?: DEFAULT_SNOOZE_MINUTES)
                .takeIf { it in SNOOZE_OPTIONS } ?: DEFAULT_SNOOZE_MINUTES,
            missedAfterMinutes = (prefs[MISSED_AFTER_MINUTES] ?: DEFAULT_MISSED_AFTER_MINUTES)
                .takeIf { it in MISSED_AFTER_OPTIONS } ?: DEFAULT_MISSED_AFTER_MINUTES,
            themeMode = prefs[THEME_MODE]?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
                ?: ThemeMode.SYSTEM,
            accentIndex = prefs[ACCENT_INDEX] ?: 0,
            proCached = prefs[PRO_CACHED] ?: false,
            lastBackupReminderAt = prefs[LAST_BACKUP_REMINDER_AT] ?: 0L
        )
    }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun setOnboardingDone(done: Boolean) = dataStore.edit { it[ONBOARDING_DONE] = done }

    suspend fun setActiveProfileId(id: Long) = dataStore.edit { it[ACTIVE_PROFILE_ID] = id }

    suspend fun setSnoozeMinutes(minutes: Int) {
        require(minutes in SNOOZE_OPTIONS)
        dataStore.edit { it[SNOOZE_MINUTES] = minutes }
    }

    suspend fun setMissedAfterMinutes(minutes: Int) {
        require(minutes in MISSED_AFTER_OPTIONS)
        dataStore.edit { it[MISSED_AFTER_MINUTES] = minutes }
    }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[THEME_MODE] = mode.name }

    suspend fun setAccentIndex(index: Int) = dataStore.edit { it[ACCENT_INDEX] = index }

    suspend fun setProCached(pro: Boolean) = dataStore.edit { it[PRO_CACHED] = pro }

    suspend fun setLastBackupReminderAt(epochMillis: Long) = dataStore.edit { it[LAST_BACKUP_REMINDER_AT] = epochMillis }

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
        const val DEFAULT_MISSED_AFTER_MINUTES = 180
        val SNOOZE_OPTIONS = listOf(5, 10, 15, 30, 60)
        val MISSED_AFTER_OPTIONS = listOf(60, 120, 180, 360)

        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
        private val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        private val MISSED_AFTER_MINUTES = intPreferencesKey("missed_after_minutes")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val ACCENT_INDEX = intPreferencesKey("accent_index")
        private val PRO_CACHED = booleanPreferencesKey("pro_cached")
        private val LAST_BACKUP_REMINDER_AT = longPreferencesKey("last_backup_reminder_at")
    }
}
