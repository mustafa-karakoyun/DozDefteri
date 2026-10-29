package app.dozdefteri.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.dozdefteri.R
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.ui.theme.StatusMissed
import app.dozdefteri.ui.theme.StatusTaken

/** Icon, color and label for a dose status. Status is never conveyed by color alone. */
data class StatusVisual(val icon: ImageVector, val color: Color, val label: Int)

@Composable
fun statusVisual(status: DoseStatus): StatusVisual = when (status) {
    DoseStatus.TAKEN -> StatusVisual(Icons.Filled.CheckCircle, StatusTaken, R.string.status_taken)
    DoseStatus.MISSED -> StatusVisual(Icons.Filled.Cancel, StatusMissed, R.string.status_missed)
    DoseStatus.SKIPPED -> StatusVisual(
        Icons.Filled.RemoveCircleOutline,
        MaterialTheme.colorScheme.onSurfaceVariant,
        R.string.status_skipped
    )
    DoseStatus.SNOOZED -> StatusVisual(
        Icons.Filled.Snooze,
        MaterialTheme.colorScheme.onSurfaceVariant,
        R.string.status_snoozed
    )
    DoseStatus.PENDING -> StatusVisual(
        Icons.Filled.Schedule,
        MaterialTheme.colorScheme.onSurfaceVariant,
        R.string.status_pending
    )
}

fun statusLabelRes(status: DoseStatus): Int = when (status) {
    DoseStatus.TAKEN -> R.string.status_taken
    DoseStatus.MISSED -> R.string.status_missed
    DoseStatus.SKIPPED -> R.string.status_skipped
    DoseStatus.SNOOZED -> R.string.status_snoozed
    DoseStatus.PENDING -> R.string.status_pending
}
