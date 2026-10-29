package app.dozdefteri

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.alarm.MaintenanceWorker
import app.dozdefteri.billing.BillingManager
import app.dozdefteri.core.di.ApplicationScope
import app.dozdefteri.data.repository.ProfileRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltAndroidApp
class DozDefteriApp : Application(), Configuration.Provider {

    @Inject lateinit var profileRepository: ProfileRepository

    @Inject lateinit var doseNotifier: DoseNotifier

    @Inject lateinit var doseSync: DoseSync

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var billingManager: BillingManager

    @Inject lateinit var clock: Clock

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        doseNotifier.createChannels()
        MaintenanceWorker.schedule(WorkManager.getInstance(this))
        billingManager.start()
        appScope.launch {
            profileRepository.ensureDefaultProfile(getString(R.string.profile_default_name), clock.instant())
            doseSync.run("app_start")
        }
    }
}
