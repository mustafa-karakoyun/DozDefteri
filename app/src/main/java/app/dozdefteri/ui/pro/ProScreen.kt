package app.dozdefteri.ui.pro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.billing.BillingMessage
import app.dozdefteri.billing.PriceState
import app.dozdefteri.ui.components.DozTopBar
import app.dozdefteri.ui.components.PrimaryButton
import app.dozdefteri.ui.components.TextAction
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

@Composable
fun ProScreen(onBack: () -> Unit, viewModel: ProViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(context.getString(messageText(message)))
        }
    }
    ProContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBuy = { context.findActivity()?.let(viewModel::purchase) },
        onRestore = viewModel::restore,
        onBack = onBack
    )
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun messageText(message: BillingMessage): Int = when (message) {
    BillingMessage.PURCHASED -> R.string.pro_message_purchased
    BillingMessage.PENDING -> R.string.pro_message_pending
    BillingMessage.RESTORED -> R.string.pro_message_restored
    BillingMessage.NOTHING_TO_RESTORE -> R.string.pro_message_nothing
    BillingMessage.ERROR -> R.string.pro_message_error
}

/** One row of the Free vs Pro table. */
private data class FeatureRow(val label: Int, val free: Boolean)

private val FEATURES = listOf(
    FeatureRow(R.string.pro_row_unlimited, free = true),
    FeatureRow(R.string.pro_row_actions, free = true),
    FeatureRow(R.string.pro_row_history, free = true),
    FeatureRow(R.string.pro_row_profiles, free = false),
    FeatureRow(R.string.pro_row_widget, free = false),
    FeatureRow(R.string.pro_row_colors, free = false)
)

@Composable
fun ProContent(
    state: ProUiState,
    snackbarHostState: SnackbarHostState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit
) {
    val c = Doz.colors
    Scaffold(
        containerColor = c.bgApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { DozTopBar(stringResource(R.string.pro_title), onNavigate = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s16)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.s16)
        ) {
            Text(
                stringResource(R.string.pro_headline_v2),
                style = Doz.type.display,
                color = c.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.pro_subtitle), style = Doz.type.bodyLarge, color = c.textSecondary)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.primaryContainer, DozShapes.card)
                    .padding(Space.s16),
                verticalArrangement = Arrangement.spacedBy(Space.s4)
            ) {
                Text(
                    stringResource(R.string.pro_free_forever),
                    style = Doz.type.titleMedium,
                    color = c.onPrimaryContainer
                )
                Text(stringResource(R.string.pro_free_list), style = Doz.type.bodyMedium, color = c.onPrimaryContainer)
            }
            FeatureTable()
            if (state.isPro) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s8)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.primary)
                    Text(
                        stringResource(R.string.pro_active_thanks),
                        style = Doz.type.titleMedium,
                        color = c.textPrimary
                    )
                }
            } else {
                val price = state.price
                PrimaryButton(
                    text = if (price is PriceState.Available) {
                        stringResource(R.string.pro_buy_with_price, price.formattedPrice)
                    } else {
                        stringResource(R.string.pro_buy)
                    },
                    onClick = onBuy,
                    enabled = price is PriceState.Available,
                    modifier = Modifier.fillMaxWidth()
                )
                if (price == PriceState.Unavailable) {
                    Text(
                        stringResource(R.string.pro_price_unavailable_short),
                        style = Doz.type.bodyMedium,
                        color = c.danger.fg,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            TextAction(
                stringResource(R.string.pro_restore),
                onClick = onRestore,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                stringResource(R.string.pro_footnote),
                style = Doz.type.caption,
                color = c.textSecondary,
                modifier = Modifier.padding(bottom = Space.s24)
            )
        }
    }
}

@Composable
private fun FeatureTable() {
    val c = Doz.colors
    Surface(
        shape = DozShapes.card,
        color = c.surfaceDefault,
        border = BorderStroke(1.dp, c.borderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceContainer)
                    .padding(horizontal = Space.s16, vertical = Space.s12)
            ) {
                HeaderCell(stringResource(R.string.pro_col_feature), Modifier.weight(1f), TextAlign.Start)
                HeaderCell(stringResource(R.string.pro_col_free), Modifier.width(72.dp), TextAlign.Center)
                HeaderCell(stringResource(R.string.pro_col_pro), Modifier.width(56.dp), TextAlign.Center)
            }
            FEATURES.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = c.borderDefault)
                val label = stringResource(row.label)
                val freeText = stringResource(if (row.free) R.string.pro_cell_yes else R.string.pro_cell_no)
                val proText = stringResource(R.string.pro_cell_yes)
                val description = stringResource(R.string.pro_row_a11y, label, freeText, proText)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = Space.s16, vertical = Space.s8)
                        .clearAndSetSemantics { contentDescription = description }
                ) {
                    Text(label, style = Doz.type.bodyLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
                    Cell(row.free, Modifier.width(72.dp))
                    Cell(true, Modifier.width(56.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign) {
    Text(text, style = Doz.type.labelMedium, color = Doz.colors.textSecondary, textAlign = align, modifier = modifier)
}

@Composable
private fun Cell(included: Boolean, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (included) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Doz.colors.primary)
        } else {
            Text("–", style = Doz.type.bodyLarge, color = Doz.colors.textSecondary)
        }
    }
}

@DozPreviews
@Composable
private fun ProPreview() {
    DozDefteriTheme {
        ProContent(
            state = ProUiState(isPro = false, price = PriceState.Available("₺149")),
            snackbarHostState = remember { SnackbarHostState() },
            onBuy = {},
            onRestore = {},
            onBack = {}
        )
    }
}
