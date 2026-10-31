package app.dozdefteri.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.MedicationForm

/** Design-token overview used to check the theme in light and dark mode. Preview only, not shipped UI. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TokenOverview() {
    val c = Doz.colors
    val t = Doz.type
    val swatches = listOf(
        "bg.app" to c.bgApp,
        "surface.default" to c.surfaceDefault,
        "surface.container" to c.surfaceContainer,
        "surface.container-high" to c.surfaceContainerHigh,
        "primary" to c.primary,
        "primary.container" to c.primaryContainer,
        "border.default" to c.borderDefault,
        "taken" to c.taken.bg,
        "skipped" to c.skipped.bg,
        "snoozed" to c.snoozed.bg,
        "missed" to c.missed.bg,
        "warning" to c.warning.bg,
        "danger" to c.danger.bg,
        "pro" to c.pro.bg
    )
    Column(
        Modifier.background(c.bgApp).padding(Space.s16),
        verticalArrangement = Arrangement.spacedBy(Space.s12)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
            verticalArrangement = Arrangement.spacedBy(Space.s8)
        ) {
            swatches.forEach { (name, color) -> Swatch(name, color) }
        }
        Text("Display", style = t.display, color = c.textPrimary)
        Text("Headline", style = t.headline, color = c.textPrimary)
        Text("Title / Large", style = t.titleLarge, color = c.textPrimary)
        Text("Title / Medium", style = t.titleMedium, color = c.textPrimary)
        Text("Body / Large", style = t.bodyLarge, color = c.textPrimary)
        Text("Body / Medium", style = t.bodyMedium, color = c.textSecondary)
        Text("Label / Large", style = t.labelLarge, color = c.textPrimary)
        Text("Caption", style = t.caption, color = c.textSecondary)
        Text("08:00 20:00", style = t.numericTime, color = c.textPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
            MedicationForm.entries.forEach {
                Icon(DozIcons.forForm(it), contentDescription = null, tint = c.textPrimary)
            }
            DoseStatus.entries.forEach {
                Icon(DozIcons.forStatus(it), contentDescription = null, tint = c.textPrimary)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
            ProfilePalette.forEach {
                Box(Modifier.size(32.dp).background(it.color, DozShapes.pill), contentAlignment = Alignment.Center) {
                    Text("A", color = it.on, style = t.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(56.dp)
                .background(color, DozShapes.small)
                .border(1.dp, Doz.colors.borderDefault, DozShapes.small)
        )
        Text(name, style = Doz.type.caption, color = Doz.colors.textSecondary)
    }
}

@DozPreviews
@Composable
private fun TokenOverviewPreview() {
    DozDefteriTheme { TokenOverview() }
}
