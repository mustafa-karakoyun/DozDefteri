package app.dozdefteri

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.ui.MainViewModel
import app.dozdefteri.ui.navigation.AppNavHost
import app.dozdefteri.ui.theme.DozDefteriTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var doseSync: DoseSync

    private val mainViewModel: MainViewModel by viewModels()

    /** Destination requested by a notification or widget; consumed by the nav host. */
    private var pendingDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingDestination = intent.getStringExtra(EXTRA_DESTINATION)
        setContent {
            val state by mainViewModel.uiState.collectAsStateWithLifecycle()
            // Nothing is drawn until settings are read once, so the start destination never flips.
            state?.let { current ->
                DozDefteriTheme(themeMode = current.themeMode, largeText = current.largeText) {
                    AppNavHost(
                        onboardingDone = current.onboardingDone,
                        pendingDestination = pendingDestination,
                        onDestinationHandled = { pendingDestination = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination = intent.getStringExtra(EXTRA_DESTINATION)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { doseSync.run("resume") }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val DESTINATION_TODAY = "today"
        const val DESTINATION_EXPORT = "export"
        const val DESTINATION_PRO = "pro"
    }
}
