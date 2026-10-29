@file:OptIn(ExperimentalMaterial3Api::class)

package app.dozdefteri.ui.pro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.billing.BillingMessage
import app.dozdefteri.billing.PriceState
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.StatusTaken

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

@Composable
fun ProContent(
    state: ProUiState,
    snackbarHostState: SnackbarHostState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pro_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.pro_headline), style = MaterialTheme.typography.headlineSmall)
            Feature(Icons.Filled.Group, stringResource(R.string.pro_feature_profiles))
            Feature(Icons.Filled.Widgets, stringResource(R.string.pro_feature_widget))
            Feature(Icons.Filled.PictureAsPdf, stringResource(R.string.pro_feature_pdf))
            Feature(Icons.Filled.Palette, stringResource(R.string.pro_feature_colors))
            Feature(Icons.Filled.SelectAll, stringResource(R.string.pro_feature_all_profiles))

            if (state.isPro) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusTaken)
                    Text(stringResource(R.string.pro_active_thanks), style = MaterialTheme.typography.titleMedium)
                }
            } else {
                when (val price = state.price) {
                    PriceState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    is PriceState.Available -> Text(
                        stringResource(R.string.pro_price, price.formattedPrice),
                        style = MaterialTheme.typography.titleLarge
                    )
                    PriceState.Unavailable -> Text(
                        stringResource(R.string.pro_price_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = onBuy,
                    enabled = state.price is PriceState.Available,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.pro_buy))
                }
            }
            OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.pro_restore))
            }
            Text(
                stringResource(R.string.pro_always_free),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun ProPreview() {
    DozDefteriTheme {
        ProContent(
            state = ProUiState(isPro = false, price = PriceState.Available("₺149,00")),
            snackbarHostState = remember { SnackbarHostState() },
            onBuy = {},
            onRestore = {},
            onBack = {}
        )
    }
}
