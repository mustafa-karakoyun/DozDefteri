package app.dozdefteri.ui.battery

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.dozdefteri.R
import app.dozdefteri.ui.components.DozCard
import app.dozdefteri.ui.components.DozTopBar
import app.dozdefteri.ui.components.SystemSettings
import app.dozdefteri.ui.components.TonalButton
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.Space

@Composable
fun BatteryGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    BatteryGuideContent(
        initial = Manufacturer.from(Build.MANUFACTURER),
        onOpenSettings = { SystemSettings.openBatteryOptimizationSettings(context) },
        onBack = onBack
    )
}

@Composable
fun BatteryGuideContent(initial: Manufacturer, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    var brand by rememberSaveable { mutableStateOf(initial) }
    Scaffold(
        containerColor = Doz.colors.bgApp,
        topBar = { DozTopBar(stringResource(R.string.battery_guide_title), onNavigate = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s16)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.s16)
        ) {
            Text(stringResource(R.string.battery_intro), style = Doz.type.bodyLarge, color = Doz.colors.textSecondary)
            DozCard {
                BatterySteps(brand, onSelect = { brand = it })
                Text(
                    stringResource(brand.details),
                    style = Doz.type.bodyMedium,
                    color = Doz.colors.textPrimary,
                    modifier = Modifier.padding(top = Space.s16)
                )
                TonalButton(
                    stringResource(R.string.battery_open_settings_short),
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .padding(top = Space.s16)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@DozPreviews
@Composable
private fun BatteryGuidePreview() {
    DozDefteriTheme { BatteryGuideContent(Manufacturer.XIAOMI, {}, {}) }
}
