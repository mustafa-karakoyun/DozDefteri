package app.dozdefteri.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.dozdefteri.R

/** Debug-only tools; callers show it only when BuildConfig.DEBUG is true. */
@Composable
fun DeveloperSection(onCreateTestDose: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.debug_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )
        OutlinedButton(
            onClick = onCreateTestDose,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.debug_create_test_dose))
        }
    }
}
