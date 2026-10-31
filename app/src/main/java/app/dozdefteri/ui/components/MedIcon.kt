package app.dozdefteri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozIcons
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes

/** 40 dp medication icon circle; [muted] is used by completed dose cards. */
@Composable
fun MedIcon(form: MedicationForm, modifier: Modifier = Modifier, muted: Boolean = false) {
    val c = Doz.colors
    Box(
        modifier = modifier
            .size(40.dp)
            .background(if (muted) c.surfaceContainerHigh else c.primaryContainer, DozShapes.pill),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            DozIcons.forForm(form),
            contentDescription = null,
            tint = if (muted) c.textSecondary else c.onPrimaryContainer,
            modifier = Modifier.size(20.dp)
        )
    }
}

@DozPreviews
@Composable
private fun MedIconPreview() {
    DozDefteriTheme {
        androidx.compose.foundation.layout.Row {
            MedIcon(MedicationForm.TABLET)
            MedIcon(MedicationForm.DROPS)
            MedIcon(MedicationForm.CAPSULE, muted = true)
        }
    }
}
