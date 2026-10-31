package app.dozdefteri.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.Space

/** Filled primary action ("Aldım", "Kaydet"): Touch.primary tall (56 dp, 64 dp with large text). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = DozShapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = Doz.colors.primary,
            contentColor = Doz.colors.textOnPrimary
        ),
        contentPadding = PaddingValues(horizontal = Space.s24),
        modifier = modifier.heightIn(min = Doz.touch.primary)
    ) {
        Text(text, style = Doz.type.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Tonal action on `primary.container` ("Güncelle", "Stok ekle", "Şimdi al"). */
@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = DozShapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = Doz.colors.primaryContainer,
            contentColor = Doz.colors.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = Space.s20),
        modifier = modifier.heightIn(min = Doz.touch.min)
    ) {
        Text(text, style = Doz.type.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Secondary outlined action ("Yedekten geri yükle", "+ Profil ekle"). */
@Composable
fun OutlinedPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = DozShapes.pill,
        border = BorderStroke(1.dp, Doz.colors.borderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Doz.colors.primary),
        modifier = modifier.heightIn(min = Doz.touch.primary)
    ) {
        Text(text, style = Doz.type.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Low-emphasis text action ("Şimdi değil", "Satın alımı geri yükle"). */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = Doz.colors.primary),
        modifier = modifier.heightIn(min = Doz.touch.min)
    ) {
        Text(text, style = Doz.type.labelLarge, textAlign = TextAlign.Center)
    }
}

@DozPreviews
@Composable
private fun ButtonsPreview() {
    DozDefteriTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
            PrimaryButton("Aldım", {})
            TonalButton("Stok ekle", {})
            OutlinedPillButton("Yedekten geri yükle", {})
            TextAction("Şimdi değil", {})
        }
    }
}
