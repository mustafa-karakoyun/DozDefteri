package app.dozdefteri.ui.medications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.core.format.DoseText
import app.dozdefteri.core.format.Formats
import app.dozdefteri.core.format.ScheduleFormatter
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.ui.components.AddStockSheet
import app.dozdefteri.ui.components.DozExtendedFab
import app.dozdefteri.ui.components.HeaderProfile
import app.dozdefteri.ui.components.MedIcon
import app.dozdefteri.ui.components.PrimaryButton
import app.dozdefteri.ui.components.ScreenHeader
import app.dozdefteri.ui.components.StockBar
import app.dozdefteri.ui.components.TonalButton
import app.dozdefteri.ui.shell.LocalHeaderProfile
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space
import app.dozdefteri.ui.today.stockLabel

data class MedicationsActions(
    val onAdd: () -> Unit = {},
    val onOpen: (Long) -> Unit = {},
    val onRestore: (Long) -> Unit = {},
    val onStartStock: (Long, Double) -> Unit = { _, _ -> },
    val onAddStock: (Long, Double) -> Unit = { _, _ -> }
)

@Composable
fun MedicationsScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    openStockFor: Long? = null,
    onStockRequestHandled: () -> Unit = {},
    viewModel: MedicationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MedicationsContent(
        state = state,
        openStockFor = openStockFor,
        onStockRequestHandled = onStockRequestHandled,
        header = LocalHeaderProfile.current,
        actions = MedicationsActions(
            onAdd = onAdd,
            onOpen = onOpen,
            onRestore = viewModel::restore,
            onStartStock = viewModel::startStockTracking,
            onAddStock = viewModel::addStock
        )
    )
}

@Composable
fun MedicationsContent(
    state: MedicationsUiState,
    header: HeaderProfile?,
    actions: MedicationsActions,
    openStockFor: Long? = null,
    onStockRequestHandled: () -> Unit = {}
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var stockSheetFor by remember { mutableStateOf<MedicationEntity?>(null) }
    // "Stok ekle" from the low-stock notification opens the sheet once the list has loaded.
    LaunchedEffect(openStockFor, state.isLoading) {
        val id = openStockFor ?: return@LaunchedEffect
        if (state.isLoading) return@LaunchedEffect
        stockSheetFor = state.active.firstOrNull { it.medication.id == id }?.medication
        onStockRequestHandled()
    }
    val c = Doz.colors
    Scaffold(
        containerColor = c.bgApp,
        topBar = { ScreenHeader(stringResource(R.string.tab_medications), header) },
        floatingActionButton = { DozExtendedFab(stringResource(R.string.add_medication), onClick = actions.onAdd) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SecondaryTabRow(
                selectedTabIndex = tab,
                containerColor = c.bgApp,
                contentColor = c.primary,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tab),
                        color = c.primary,
                        height = 3.dp
                    )
                },
                divider = { HorizontalDivider(color = c.borderDefault) }
            ) {
                TabLabel(stringResource(R.string.tab_active_count, state.active.size), tab == 0) { tab = 0 }
                TabLabel(stringResource(R.string.tab_archive_count, state.archived.size), tab == 1) { tab = 1 }
            }
            val items = if (tab == 0) state.active else state.archived
            if (!state.isLoading && items.isEmpty()) {
                EmptyList(
                    if (tab == 0) R.string.empty_medications_body else R.string.empty_archive_body,
                    tab == 0,
                    actions.onAdd
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = Space.s16,
                        end = Space.s16,
                        top = Space.s16,
                        bottom = 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.s12)
                ) {
                    items(items, key = { it.medication.id }) { item ->
                        MedicationCard(
                            item = item,
                            archived = tab == 1,
                            onOpen = { actions.onOpen(item.medication.id) },
                            onRestore = { actions.onRestore(item.medication.id) },
                            onAddStock = { stockSheetFor = item.medication }
                        )
                    }
                }
            }
        }
    }
    stockSheetFor?.let { medication ->
        AddStockSheet(
            medicationName = medication.name,
            unit = medication.doseUnit,
            onAdd = { amount ->
                stockSheetFor = null
                if (medication.stockQuantity == null) {
                    actions.onStartStock(medication.id, amount)
                } else {
                    actions.onAddStock(medication.id, amount)
                }
            },
            onDismiss = { stockSheetFor = null }
        )
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        selectedContentColor = Doz.colors.primary,
        unselectedContentColor = Doz.colors.textSecondary,
        text = { Text(text, style = Doz.type.labelLarge) },
        modifier = Modifier.heightIn(min = Doz.touch.min)
    )
}

@Composable
private fun EmptyList(textRes: Int, showAdd: Boolean, onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(Space.s24), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s16)
        ) {
            Text(
                stringResource(textRes),
                style = Doz.type.bodyLarge,
                color = Doz.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (showAdd) PrimaryButton(stringResource(R.string.add_medication), onClick = onAdd)
        }
    }
}

@Composable
private fun MedicationCard(
    item: MedicationListItem,
    archived: Boolean,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
    onAddStock: () -> Unit
) {
    val c = Doz.colors
    val context = LocalContext.current
    val med = item.medication
    val first = DoseText.strength(med.strengthAmount, med.strengthUnit)
        ?: Formats.dose(med.doseAmount, med.doseUnit)
    val subtitle = "$first${DoseText.SEPARATOR}${ScheduleFormatter.summary(context, item.schedule)}"
    Surface(
        shape = DozShapes.card,
        color = c.surfaceDefault,
        border = BorderStroke(1.dp, c.borderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s12)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s16),
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpen)
            ) {
                MedIcon(med.form, muted = archived)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                    Text(med.name, style = Doz.type.titleMedium, color = c.textPrimary)
                    Text(subtitle, style = Doz.type.bodyMedium, color = c.textSecondary)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
            }
            val stock = med.stockQuantity
            when {
                archived -> TonalButton(stringResource(R.string.restore_to_active), onClick = onRestore)
                stock == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s8),
                    modifier = Modifier
                        .heightIn(min = Doz.touch.min)
                        .clickable(role = Role.Button, onClick = onAddStock)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = c.textSecondary)
                    Text(
                        stringResource(R.string.stock_missing_add),
                        style = Doz.type.labelLarge,
                        color = c.textSecondary
                    )
                }
                else -> StockBar(
                    label = stockLabel(stock, med.doseUnit, if (item.isAsNeeded) null else item.remainingDays),
                    fraction = item.stockFraction,
                    low = item.isLowStock,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onAddStock)
                )
            }
        }
    }
}

@DozPreviews
@Composable
private fun MedicationsPreview() {
    fun med(
        id: Long,
        name: String,
        stock: Double?,
        threshold: Double? = null,
        form: MedicationForm = MedicationForm.TABLET
    ) =
        MedicationEntity(
            id = id,
            profileId = 1,
            name = name,
            doseAmount = 1.0,
            doseUnit = "tablet",
            form = form,
            note = null,
            stockQuantity = stock,
            lowStockThreshold = threshold,
            createdAt = 0,
            updatedAt = 0,
            strengthAmount = 500.0,
            strengthUnit = "mg"
        )
    DozDefteriTheme {
        MedicationsContent(
            state = MedicationsUiState(
                isLoading = false,
                active = listOf(
                    MedicationListItem(med(1, "Metformin", 38.0), null, 19, false, 0.63f),
                    MedicationListItem(med(2, "Atorvastatin", 4.0, 8.0), null, 4, true, 0.13f),
                    MedicationListItem(med(3, "D vitamini", null, form = MedicationForm.DROPS), null, null, false, 0f)
                )
            ),
            header = HeaderProfile("Ayşe", 0) {},
            actions = MedicationsActions()
        )
    }
}
