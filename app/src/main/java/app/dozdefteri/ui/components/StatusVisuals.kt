package app.dozdefteri.ui.components

import app.dozdefteri.R
import app.dozdefteri.domain.model.DoseStatus

/** Localized label of a dose status (used by exports, chips use [statusLabel]). */
fun statusLabelRes(status: DoseStatus): Int = when (status) {
    DoseStatus.TAKEN -> R.string.status_taken
    DoseStatus.MISSED -> R.string.status_missed
    DoseStatus.SKIPPED -> R.string.status_skipped
    DoseStatus.SNOOZED -> R.string.status_snoozed
    DoseStatus.PENDING -> R.string.status_pending
}
