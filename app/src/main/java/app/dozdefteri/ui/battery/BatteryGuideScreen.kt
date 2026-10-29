@file:OptIn(ExperimentalMaterial3Api::class)

package app.dozdefteri.ui.battery

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.dozdefteri.R
import app.dozdefteri.ui.components.SystemSettings
import app.dozdefteri.ui.theme.DozDefteriTheme
import java.util.Locale

/** Manufacturer families with their own battery-saving behaviour (plan 6.10). */
enum class Manufacturer(val brands: Set<String>, val titleRes: Int?, val stepsRes: Int) {
    XIAOMI(setOf("xiaomi", "redmi", "poco"), R.string.battery_brand_xiaomi, R.string.battery_steps_xiaomi),
    SAMSUNG(setOf("samsung"), R.string.battery_brand_samsung, R.string.battery_steps_samsung),
    HUAWEI(setOf("huawei", "honor"), R.string.battery_brand_huawei, R.string.battery_steps_huawei),
    OPPO(setOf("oppo", "realme", "oneplus"), R.string.battery_brand_oppo, R.string.battery_steps_oppo),
    VIVO(setOf("vivo"), R.string.battery_brand_vivo, R.string.battery_steps_vivo),
    OTHER(emptySet(), null, R.string.battery_steps_generic);

    companion object {
        fun from(manufacturer: String): Manufacturer {
            val value = manufacturer.lowercase(Locale.ROOT)
            return entries.firstOrNull { it.brands.any { brand -> value.contains(brand) } } ?: OTHER
        }
    }
}

@Composable
fun BatteryGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    BatteryGuideContent(
        manufacturer = Manufacturer.from(Build.MANUFACTURER),
        onOpenSettings = { SystemSettings.openBatteryOptimizationSettings(context) },
        onBack = onBack
    )
}

@Composable
fun BatteryGuideContent(manufacturer: Manufacturer, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.battery_guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.battery_intro), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.battery_open_settings))
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        manufacturer.titleRes?.let { stringResource(it) }
                            ?: stringResource(R.string.battery_brand_generic),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(stringResource(manufacturer.stepsRes), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun BatteryGuidePreview() {
    DozDefteriTheme {
        BatteryGuideContent(manufacturer = Manufacturer.XIAOMI, onOpenSettings = {}, onBack = {})
    }
}
