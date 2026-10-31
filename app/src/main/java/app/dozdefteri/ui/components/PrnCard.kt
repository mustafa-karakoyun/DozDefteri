package app.dozdefteri.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.dozdefteri.R
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.Space

/** As-needed medication: "Parasetamol / 500 mg · son: dün 22:10" with a tonal "Şimdi al". */
@Composable
fun PrnCard(
    name: String,
    detail: String,
    form: MedicationForm,
    onTakeNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    DozCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s16)) {
            MedIcon(form)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                Text(name, style = Doz.type.titleMedium, color = Doz.colors.textPrimary)
                Text(detail, style = Doz.type.bodyMedium, color = Doz.colors.textSecondary)
            }
            TonalButton(stringResource(R.string.take_now_short), onClick = onTakeNow)
        }
    }
}

@DozPreviews
@Composable
private fun PrnCardPreview() {
    DozDefteriTheme { PrnCard("Parasetamol", "500 mg · son: dün 22:10", MedicationForm.TABLET, {}) }
}
