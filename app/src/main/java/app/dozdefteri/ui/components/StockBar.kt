package app.dozdefteri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

/**
 * Stock read three ways: a 6 dp bar, the amount and the days-left estimate in [label] (e.g. "38 tablet · ~19 gün
 * yeter"). [fraction] is 0..1. When [low] the bar and text use the warning color and get a warning icon.
 */
@Composable
fun StockBar(label: String, fraction: Float, low: Boolean, modifier: Modifier = Modifier) {
    val c = Doz.colors
    val color = if (low) c.warning.fg else c.textSecondary
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(Space.s8)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
            if (low) {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(label, style = if (low) Doz.type.labelLarge else Doz.type.bodyMedium, color = color)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(c.surfaceContainerHigh, DozShapes.pill)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(if (low) c.warning.fg else c.primary, DozShapes.pill)
            )
        }
    }
}

@DozPreviews
@Composable
private fun StockBarPreview() {
    DozDefteriTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s16)) {
            StockBar("38 tablet · ~19 gün yeter", 0.63f, low = false)
            StockBar("4 tablet · ~4 gün yeter", 0.13f, low = true)
        }
    }
}
