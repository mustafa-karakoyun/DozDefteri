package app.dozdefteri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.dozdefteri.R
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozIcons
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

/** Chip variants: the five dose statuses plus low stock. */
sealed interface ChipKind {
    data class Dose(val status: DoseStatus) : ChipKind
    data object LowStock : ChipKind
}

/** 28 dp status chip with a 16 dp icon; status is always icon + text, never color alone. */
@Composable
fun StatusChip(kind: ChipKind, modifier: Modifier = Modifier, label: String? = null) {
    val style = chipStyle(kind)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
        modifier = modifier
            .heightIn(min = 28.dp)
            .background(style.bg, DozShapes.pill)
            .padding(start = Space.s8, end = Space.s12)
    ) {
        Icon(style.icon, contentDescription = null, tint = style.fg, modifier = Modifier.size(16.dp))
        Text(label ?: stringResource(style.text), style = Doz.type.labelMedium, color = style.fg)
    }
}

private data class ChipStyle(val bg: Color, val fg: Color, val icon: ImageVector, val text: Int)

@Composable
private fun chipStyle(kind: ChipKind): ChipStyle {
    val c = Doz.colors
    return when (kind) {
        ChipKind.LowStock ->
            ChipStyle(c.warning.bg, c.warning.fg, Icons.Rounded.ErrorOutline, R.string.status_low_stock)
        is ChipKind.Dose -> {
            val icon = DozIcons.forStatus(kind.status)
            when (kind.status) {
                DoseStatus.PENDING -> ChipStyle(c.surfaceContainerHigh, c.pendingFg, icon, R.string.status_pending)
                DoseStatus.TAKEN -> ChipStyle(c.taken.bg, c.taken.fg, icon, R.string.status_taken)
                DoseStatus.SNOOZED -> ChipStyle(c.snoozed.bg, c.snoozed.fg, icon, R.string.status_snoozed)
                DoseStatus.SKIPPED -> ChipStyle(c.skipped.bg, c.skipped.fg, icon, R.string.status_skipped)
                DoseStatus.MISSED -> ChipStyle(c.missed.bg, c.missed.fg, icon, R.string.status_missed)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@DozPreviews
@Composable
private fun StatusChipPreview() {
    DozDefteriTheme {
        FlowRow {
            DoseStatus.entries.forEach { StatusChip(ChipKind.Dose(it), Modifier.padding(Space.s4)) }
            StatusChip(ChipKind.LowStock, Modifier.padding(Space.s4))
        }
    }
}
