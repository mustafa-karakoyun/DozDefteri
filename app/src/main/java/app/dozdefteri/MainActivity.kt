package app.dozdefteri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.dozdefteri.ui.navigation.AppNavHost
import app.dozdefteri.ui.theme.DozDefteriTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DozDefteriTheme {
                AppNavHost()
            }
        }
    }
}
