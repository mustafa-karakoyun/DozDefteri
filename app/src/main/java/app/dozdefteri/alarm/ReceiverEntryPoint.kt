package app.dozdefteri.alarm

import android.content.Context
import app.dozdefteri.data.prefs.SettingsRepository
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.ProfileRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Dependencies used by the broadcast receivers, obtained without field injection. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun doseSync(): DoseSync
    fun doseRepository(): DoseRepository
    fun profileRepository(): ProfileRepository
    fun settingsRepository(): SettingsRepository
    fun notifier(): DoseNotifier
    fun clock(): Clock

    companion object {
        fun get(context: Context): ReceiverEntryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
    }
}
