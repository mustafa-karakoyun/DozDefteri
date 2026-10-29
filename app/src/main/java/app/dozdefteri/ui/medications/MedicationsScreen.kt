@file:OptIn(ExperimentalMaterial3Api::class)

package app.dozdefteri.ui.medications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.core.format.ScheduleFormatter
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.ui.components.EmptyState
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.StatusMissed

@Composable
fun MedicationsScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: MedicationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MedicationsContent(state = state, onAdd = onAdd, onOpen = onOpen, onRestore = viewModel::restore)
}

@Composable
fun MedicationsContent(
    state: MedicationsUiState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onRestore: (Long) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_medications)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_medication)) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_active)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_archive)) })
            }
            val items = if (tab == 0) state.active else state.archived
            if (!state.isLoading && items.isEmpty()) {
                if (tab == 0) {
                    EmptyState(
                        icon = Icons.Filled.Medication,
                        text = stringResource(R.string.empty_medications),
                        actionLabel = stringResource(R.string.add_medication),
                        onAction = onAdd
                    )
                } else {
                    EmptyState(icon = Icons.Filled.Inventory2, text = stringResource(R.string.empty_archive))
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(items, key = { it.medication.id }) { item ->
                        MedicationRow(
                            item = item,
                            archived = tab == 1,
                            onOpen = { onOpen(item.medication.id) },
                            onRestore = { onRestore(item.medication.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationRow(item: MedicationListItem, archived: Boolean, onOpen: () -> Unit, onRestore: () -> Unit) {
    val context = LocalContext.current
    val medication = item.medication
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onOpen, role = Role.Button)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(medication.name, style = MaterialTheme.typography.titleMedium)
            Text(
                Formats.dose(medication.doseAmount, medication.doseUnit),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                ScheduleFormatter.summary(context, item.schedule),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            medication.stockQuantity?.let { stock -> StockBadge(stock, medication.doseUnit, item) }
        }
        if (archived) {
            Spacer(Modifier.size(8.dp))
            OutlinedButton(onClick = onRestore, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.restore))
            }
        }
    }
}

@Composable
fun StockBadge(stock: Double, unit: String, item: MedicationListItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val color = if (item.isLowStock) StatusMissed else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(
            if (item.isLowStock) Icons.Filled.Warning else Icons.Filled.Inventory2,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        val text = buildString {
            append(
                if (item.isLowStock) {
                    stringResource(R.string.stock_low_left, Formats.dose(stock, unit))
                } else {
                    stringResource(R.string.stock_left, Formats.dose(stock, unit))
                }
            )
            item.remainingDays?.let { days ->
                append(" · ")
                append(pluralStringResource(R.plurals.days_left, days, days))
            }
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun MedicationsPreview() {
    val medication = MedicationEntity(
        id = 1,
        profileId = 1,
        name = "Vitamin D",
        doseAmount = 1.0,
        doseUnit = "tablet",
        form = MedicationForm.TABLET,
        note = null,
        stockQuantity = 4.0,
        lowStockThreshold = 5.0,
        createdAt = 0,
        updatedAt = 0
    )
    DozDefteriTheme {
        MedicationsContent(
            state = MedicationsUiState(
                isLoading = false,
                active = listOf(MedicationListItem(medication, null, remainingDays = 4, isLowStock = true))
            ),
            onAdd = {},
            onOpen = {},
            onRestore = {}
        )
    }
}
