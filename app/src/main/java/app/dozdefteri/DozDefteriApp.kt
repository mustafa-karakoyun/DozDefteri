package app.dozdefteri

import android.app.Application
import app.dozdefteri.core.di.ApplicationScope
import app.dozdefteri.data.repository.ProfileRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltAndroidApp
class DozDefteriApp : Application() {

    @Inject lateinit var profileRepository: ProfileRepository

    @Inject lateinit var clock: Clock

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            profileRepository.ensureDefaultProfile(getString(R.string.profile_default_name), clock.instant())
        }
    }
}
