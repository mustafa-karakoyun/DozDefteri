@file:OptIn(ExperimentalMaterial3Api::class)

package app.dozdefteri.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import app.dozdefteri.BuildConfig
import app.dozdefteri.R

@Composable
fun SettingsScreen(debugViewModel: DebugViewModel = hiltViewModel()) {
    val testName = stringResource(R.string.debug_test_medication_name)
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (BuildConfig.DEBUG) {
                DeveloperSection(onCreateTestDose = { debugViewModel.createTestDose(testName) })
            }
        }
    }
}
