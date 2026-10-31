package app.dozdefteri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.dozdefteri.R
import app.dozdefteri.ui.theme.ColorPair
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

enum class BannerKind { Warning, Info, Error }

/** Tinted inline message with an optional action ("Düzelt") and optional dismiss button. */
@Composable
fun InlineBanner(
    kind: BannerKind,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    icon: ImageVector? = null
) {
    val c = Doz.colors
    val pair: ColorPair = when (kind) {
        BannerKind.Warning -> c.warning
        BannerKind.Info -> ColorPair(c.primaryContainer, c.onPrimaryContainer)
        BannerKind.Error -> c.danger
    }
    val leading = icon ?: when (kind) {
        BannerKind.Warning -> Icons.Rounded.Schedule
        BannerKind.Info -> Icons.Rounded.Info
        BannerKind.Error -> Icons.Rounded.ErrorOutline
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s12),
        modifier = modifier
            .fillMaxWidth()
            .background(pair.bg, DozShapes.card)
            .padding(start = Space.s16, end = Space.s4, top = Space.s8, bottom = Space.s8)
    ) {
        Icon(leading, contentDescription = null, tint = pair.fg)
        Text(text, style = Doz.type.bodyMedium, color = pair.fg, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, colors = ButtonDefaults.textButtonColors(contentColor = pair.fg)) {
                Text(actionLabel, style = Doz.type.labelLarge)
            }
        }
        if (onDismiss != null) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.dismiss), tint = pair.fg)
            }
        }
    }
}

@DozPreviews
@Composable
private fun InlineBannerPreview() {
    DozDefteriTheme {
        InlineBanner(
            BannerKind.Warning,
            "Hatırlatmalar birkaç dakika gecikebilir.",
            actionLabel = "Düzelt",
            onAction = {}
        )
    }
}
