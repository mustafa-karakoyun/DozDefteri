package app.dozdefteri.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.dozdefteri.R
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.Space

/** Warning card "Atorvastatin azalıyor" with a [StockBar] and a tonal "Stok ekle". */
@Composable
fun LowStockCard(
    name: String,
    stockLabel: String,
    fraction: Float,
    onAddStock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val warning = Doz.colors.warning
    DozCard(modifier, color = warning.bg, borderColor = null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
            modifier = Modifier.semantics { heading() }
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = warning.fg)
            Text(stringResource(R.string.low_stock_title, name), style = Doz.type.titleMedium, color = warning.fg)
        }
        StockBar(stockLabel, fraction, low = true, modifier = Modifier.padding(top = Space.s12))
        TonalButton(
            stringResource(R.string.add_stock),
            onClick = onAddStock,
            modifier = Modifier.padding(top = Space.s12)
        )
    }
}

@DozPreviews
@Composable
private fun LowStockCardPreview() {
    DozDefteriTheme { LowStockCard("Atorvastatin", "4 tablet · ~4 gün yeter", 0.13f, {}) }
}
