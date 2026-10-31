@file:OptIn(ExperimentalMaterial3Api::class)

package app.dozdefteri.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.dozdefteri.R
import app.dozdefteri.ui.medications.MedicationFormValidator
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space
import java.time.LocalTime

/**
 * "Ne zaman aldın?" sheet used by "Güncelle", "Saati düzelt" and "Aldım olarak işaretle".
 * [onSkip] adds a "mark as skipped" option (missed doses).
 */
@Composable
fun TakenTimeSheet(
    medicationName: String,
    initial: LocalTime,
    onSave: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val c = Doz.colors
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    )
    ModalBottomSheet(onDismissRequest = onDismiss, shape = DozShapes.sheet, containerColor = c.surfaceDefault) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.s16, vertical = Space.s8),
            verticalArrangement = Arrangement.spacedBy(Space.s16),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.taken_time_title, medicationName),
                style = Doz.type.titleLarge,
                color = c.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
            )
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    selectorColor = c.primary,
                    timeSelectorSelectedContainerColor = c.primaryContainer,
                    timeSelectorSelectedContentColor = c.onPrimaryContainer
                )
            )
            PrimaryButton(
                stringResource(R.string.taken_time_save),
                onClick = { onSave(LocalTime.of(state.hour, state.minute)) },
                modifier = Modifier.fillMaxWidth()
            )
            if (onSkip != null) {
                OutlinedPillButton(
                    stringResource(R.string.dose_mark_skipped),
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** "Stok ekle" sheet: how many units were added. */
@Composable
fun AddStockSheet(medicationName: String, unit: String, onAdd: (Double) -> Unit, onDismiss: () -> Unit) {
    val c = Doz.colors
    var text by rememberSaveable { mutableStateOf("") }
    val amount = MedicationFormValidator.parseDecimal(text)?.takeIf { it > 0.0 }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = DozShapes.sheet, containerColor = c.surfaceDefault) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.s16, vertical = Space.s8),
            verticalArrangement = Arrangement.spacedBy(Space.s16)
        ) {
            Text(
                stringResource(R.string.add_stock_title, medicationName),
                style = Doz.type.titleLarge,
                color = c.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
            Column {
                FieldLabel(stringResource(R.string.add_stock_amount, unit))
                DozTextField(value = text, onValueChange = { text = it }, numeric = true)
            }
            PrimaryButton(
                stringResource(R.string.add_stock),
                onClick = { amount?.let(onAdd) },
                enabled = amount != null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
