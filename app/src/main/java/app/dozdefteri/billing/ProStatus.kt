package app.dozdefteri.billing

import app.dozdefteri.BuildConfig
import app.dozdefteri.core.di.ApplicationScope
import app.dozdefteri.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read side of the Pro entitlement used by every feature gate. Backed by the cached purchase state that
 * [BillingManager] keeps up to date; `BuildConfig.DEBUG_FORCE_PRO` forces it on.
 */
@Singleton
class ProStatus @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationScope scope: CoroutineScope
) {
    val isPro: StateFlow<Boolean> = settingsRepository.settings
        .map { BuildConfig.DEBUG_FORCE_PRO || it.proCached }
        .stateIn(scope, SharingStarted.Eagerly, BuildConfig.DEBUG_FORCE_PRO)

    /** Reads the stored state directly; use where the flow may not have loaded yet (e.g. widgets). */
    suspend fun current(): Boolean = BuildConfig.DEBUG_FORCE_PRO || settingsRepository.current().proCached

    companion object {
        /** Number of profiles available without Pro. */
        const val FREE_PROFILE_LIMIT = 1
        const val PRODUCT_ID = "pro_lifetime"
    }
}
