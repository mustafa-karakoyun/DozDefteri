package app.dozdefteri.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozIcons
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.Space

/** Group header ("Sabah", "Öğle", "Akşam", "Gerektiğinde"); announced as a heading. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s8),
        modifier = modifier
            .padding(top = Space.s12, bottom = Space.s4)
            .semantics { heading() }
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Doz.colors.textSecondary, modifier = Modifier.size(20.dp))
        }
        Text(text, style = Doz.type.labelLarge, color = Doz.colors.textSecondary)
    }
}

@DozPreviews
@Composable
private fun SectionHeaderPreview() {
    DozDefteriTheme { SectionHeader("Sabah", icon = DozIcons.Morning) }
}
