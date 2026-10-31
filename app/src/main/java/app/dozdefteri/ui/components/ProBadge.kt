package app.dozdefteri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.dozdefteri.R
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

/** Violet "☆ Pro" badge next to Pro-only features. */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    val pro = Doz.colors.pro
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
        modifier = modifier
            .heightIn(min = 28.dp)
            .background(pro.bg, DozShapes.pill)
            .padding(horizontal = Space.s8)
    ) {
        Icon(Icons.Rounded.StarOutline, contentDescription = null, tint = pro.fg, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.pro_badge), style = Doz.type.labelMedium, color = pro.fg)
    }
}

@DozPreviews
@Composable
private fun ProBadgePreview() {
    DozDefteriTheme { ProBadge() }
}
