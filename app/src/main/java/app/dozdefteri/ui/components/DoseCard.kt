package app.dozdefteri.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.dozdefteri.R
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

/** Everything a [DoseCard] shows; texts are already formatted for the current locale. */
data class DoseCardModel(
    val name: String,
    /** e.g. "500 mg · 1 tablet · tok karnına". */
    val doseLine: String,
    val form: MedicationForm,
    val status: DoseStatus,
    /** Planned time, e.g. "08:00". */
    val plannedTime: String,
    /** New time of a snoozed dose, e.g. "08:10". */
    val snoozedTime: String? = null,
    /** When the dose was taken, already with its suffix, e.g. "08:04'te". */
    val takenTimeLabel: String? = null,
    val canUndo: Boolean = false
)

data class DoseCardActions(
    val onTake: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onMarkNotTaken: () -> Unit = {},
    val onFixTime: () -> Unit = {},
    /** Opens the "when did you take it?" sheet for missed or skipped doses. */
    val onUpdate: () -> Unit = {},
    val onMarkSkipped: () -> Unit = {}
)

/**
 * The core card of the design in its five states (plan 2.1). Open doses (pending, snoozed) show a filled
 * "Aldım"; completed and missed doses show an overflow menu. Missed doses add an explanation box with "Güncelle".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoseCard(model: DoseCardModel, actions: DoseCardActions, modifier: Modifier = Modifier) {
    val c = Doz.colors
    val open = model.status == DoseStatus.PENDING || model.status == DoseStatus.SNOOZED
    val completed = model.status == DoseStatus.TAKEN || model.status == DoseStatus.SKIPPED
    val container by animateColorAsState(if (completed) c.surfaceContainer else c.surfaceDefault, label = "doseCard")
    val border: Color? = when {
        completed -> null
        model.status == DoseStatus.MISSED -> c.missed.fg
        else -> c.borderDefault
    }
    val statusText = stringResource(statusLabel(model.status))
    val timeText = when (model.status) {
        DoseStatus.TAKEN -> model.takenTimeLabel ?: model.plannedTime
        DoseStatus.SNOOZED -> model.snoozedTime?.let { "${model.plannedTime} → $it" } ?: model.plannedTime
        else -> model.plannedTime
    }
    val description = stringResource(R.string.dose_card_a11y, model.name, model.doseLine, timeText, statusText)

    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = DozShapes.card,
        color = container,
        contentColor = c.textPrimary,
        border = border?.let { BorderStroke(1.dp, it) }
    ) {
        Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s12)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s16)
            ) {
                MedIcon(model.form, muted = completed)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = description },
                    verticalArrangement = Arrangement.spacedBy(Space.s4)
                ) {
                    Text(model.name, style = Doz.type.titleMedium, color = c.textPrimary)
                    Text(model.doseLine, style = Doz.type.bodyMedium, color = c.textSecondary)
                    if (model.status == DoseStatus.SNOOZED) {
                        Text(timeText, style = Doz.type.doseTime, color = c.textPrimary)
                        StatusChip(ChipKind.Dose(model.status))
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Space.s8),
                            verticalArrangement = Arrangement.spacedBy(Space.s4),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(timeText, style = Doz.type.doseTime, color = c.textPrimary)
                            StatusChip(ChipKind.Dose(model.status))
                        }
                    }
                }
                if (open) {
                    PrimaryButton(stringResource(R.string.action_taken), onClick = actions.onTake)
                } else {
                    DoseMenu(model, actions)
                }
            }
            if (model.status == DoseStatus.MISSED) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.missed.bg, DozShapes.field)
                        .padding(Space.s12),
                    verticalArrangement = Arrangement.spacedBy(Space.s8)
                ) {
                    Text(stringResource(R.string.dose_missed_hint), style = Doz.type.bodyMedium, color = c.missed.fg)
                    TonalButton(stringResource(R.string.dose_update), onClick = actions.onUpdate)
                }
            }
        }
    }
}

@Composable
private fun DoseMenu(model: DoseCardModel, actions: DoseCardActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.dose_menu, model.name),
                tint = Doz.colors.textSecondary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val item: @Composable (Int, () -> Unit) -> Unit = { label, action ->
                DropdownMenuItem(
                    text = { Text(stringResource(label), style = Doz.type.bodyMedium) },
                    onClick = {
                        expanded = false
                        action()
                    }
                )
            }
            when (model.status) {
                DoseStatus.TAKEN -> {
                    item(R.string.dose_mark_not_taken, actions.onMarkNotTaken)
                    item(R.string.dose_fix_time, actions.onFixTime)
                }
                DoseStatus.SKIPPED -> item(R.string.dose_mark_taken, actions.onUpdate)
                DoseStatus.MISSED -> {
                    item(R.string.dose_mark_taken, actions.onUpdate)
                    item(R.string.dose_mark_skipped, actions.onMarkSkipped)
                }
                DoseStatus.PENDING, DoseStatus.SNOOZED -> Unit
            }
            if (model.canUndo) item(R.string.undo, actions.onUndo)
        }
    }
}

fun statusLabel(status: DoseStatus): Int = when (status) {
    DoseStatus.PENDING -> R.string.status_pending
    DoseStatus.TAKEN -> R.string.status_taken
    DoseStatus.SNOOZED -> R.string.status_snoozed
    DoseStatus.SKIPPED -> R.string.status_skipped
    DoseStatus.MISSED -> R.string.status_missed
}

@DozPreviews
@Composable
private fun DoseCardPreview() {
    val base = DoseCardModel(
        name = "Metformin",
        doseLine = "500 mg · 1 tablet · tok karnına",
        form = MedicationForm.TABLET,
        status = DoseStatus.PENDING,
        plannedTime = "08:00"
    )
    DozDefteriTheme {
        Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s12)) {
            DoseCard(base, DoseCardActions())
            DoseCard(
                base.copy(status = DoseStatus.TAKEN, takenTimeLabel = "08:04'te", canUndo = true),
                DoseCardActions()
            )
            DoseCard(base.copy(status = DoseStatus.SNOOZED, snoozedTime = "08:10"), DoseCardActions())
            DoseCard(base.copy(status = DoseStatus.SKIPPED), DoseCardActions())
            DoseCard(base.copy(status = DoseStatus.MISSED), DoseCardActions())
        }
    }
}
