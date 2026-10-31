package app.dozdefteri.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes

/** "+ İlaç ekle" on `primary.container`; the FAB is one of the two places that use the Elevation shadow. */
@Composable
fun DozExtendedFab(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        text = { Text(text, style = Doz.type.labelLarge) },
        shape = DozShapes.card,
        containerColor = Doz.colors.primaryContainer,
        contentColor = Doz.colors.onPrimaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(),
        modifier = modifier
    )
}

@DozPreviews
@Composable
private fun FabPreview() {
    DozDefteriTheme { DozExtendedFab("İlaç ekle", {}) }
}
