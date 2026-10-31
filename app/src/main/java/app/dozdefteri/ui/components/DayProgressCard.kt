package app.dozdefteri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.dozdefteri.R
import app.dozdefteri.core.format.TurkishSuffix
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

/**
 * "5 dozdan 2'si alındı" with one segment per scheduled dose of the day, in time order, and the next dose below.
 * Skipped doses count as completed but not taken and use the neutral pending track.
 */
@Composable
fun DayProgressCard(segments: List<DoseStatus>, nextLine: String?, modifier: Modifier = Modifier) {
    val c = Doz.colors
    val taken = segments.count { it == DoseStatus.TAKEN }
    DozCard(modifier) {
        Text(
            pluralStringResource(
                R.plurals.day_progress,
                segments.size,
                segments.size,
                TurkishSuffix.countPossessive(taken)
            ),
            style = Doz.type.titleMedium,
            color = c.textPrimary,
            modifier = Modifier.semantics { heading() }
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = Space.s12),
            horizontalArrangement = Arrangement.spacedBy(Space.s4)
        ) {
            segments.forEach { status ->
                val color = when (status) {
                    DoseStatus.TAKEN -> c.primary
                    DoseStatus.SNOOZED -> c.snoozed.fg
                    DoseStatus.MISSED -> c.missed.fg
                    DoseStatus.PENDING, DoseStatus.SKIPPED -> c.surfaceContainerHigh
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(color, DozShapes.pill)
                )
            }
        }
        if (nextLine != null) {
            Text(
                nextLine,
                style = Doz.type.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.padding(top = Space.s12)
            )
        }
    }
}

@DozPreviews
@Composable
private fun DayProgressPreview() {
    DozDefteriTheme {
        Column(Modifier.padding(Space.s16)) {
            DayProgressCard(
                listOf(DoseStatus.TAKEN, DoseStatus.TAKEN, DoseStatus.SNOOZED, DoseStatus.PENDING, DoseStatus.PENDING),
                "Sıradaki: D vitamini, 13:10 (ertelendi)"
            )
        }
    }
}
