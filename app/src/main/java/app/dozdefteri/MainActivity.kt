package app.dozdefteri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.ui.navigation.AppNavHost
import app.dozdefteri.ui.theme.DozDefteriTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var doseSync: DoseSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DozDefteriTheme {
                AppNavHost()
            }
        }
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
