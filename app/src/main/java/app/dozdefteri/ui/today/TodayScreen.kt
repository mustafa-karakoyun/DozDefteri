package app.dozdefteri.ui.today

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.core.format.DoseText
import app.dozdefteri.core.format.Formats
import app.dozdefteri.core.format.TurkishSuffix
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.components.AddStockSheet
import app.dozdefteri.ui.components.BannerKind
import app.dozdefteri.ui.components.DayProgressCard
import app.dozdefteri.ui.components.DoseCard
import app.dozdefteri.ui.components.DoseCardActions
import app.dozdefteri.ui.components.DoseCardModel
import app.dozdefteri.ui.components.DozExtendedFab
import app.dozdefteri.ui.components.HeaderProfile
import app.dozdefteri.ui.components.InlineBanner
import app.dozdefteri.ui.components.LowStockCard
import app.dozdefteri.ui.components.PermissionSnapshot
import app.dozdefteri.ui.components.PrimaryButton
import app.dozdefteri.ui.components.PrnCard
import app.dozdefteri.ui.components.ScreenHeader
import app.dozdefteri.ui.components.SectionHeader
import app.dozdefteri.ui.components.SystemSettings
import app.dozdefteri.ui.components.TakenTimeSheet
import app.dozdefteri.ui.components.rememberPermissionSnapshot
import app.dozdefteri.ui.shell.LocalHeaderProfile
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozIcons
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.Space
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TodayActions(
    val onTake: (TodayDose) -> Unit = {},
    val onUndo: (Long) -> Unit = {},
    val onMarkSkipped: (Long) -> Unit = {},
    val onMarkTakenAt: (TodayDose, LocalTime) -> Unit = { _, _ -> },
    val onTakeAsNeeded: (Long) -> Unit = {},
    val onAddStock: (Long, Double) -> Unit = { _, _ -> },
    val onAddMedication: () -> Unit = {},
    val onFixExactAlarms: () -> Unit = {},
    val onFixNotifications: () -> Unit = {}
)

@Composable
fun TodayScreen(onAddMedication: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberPermissionSnapshot()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val takenFormat = stringResource(R.string.snackbar_taken)
    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.Taken -> {
                    val result = snackbarHostState.showSnackbar(
                        message = String.format(takenFormat, event.medicationName),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undo(event.eventId)
                }
            }
        }
    }
    TodayContent(
        state = state,
        header = LocalHeaderProfile.current,
        permissions = permissions,
        snackbarHostState = snackbarHostState,
        actions = TodayActions(
            onTake = viewModel::take,
            onUndo = viewModel::undo,
            onMarkSkipped = viewModel::markSkipped,
            onMarkTakenAt = viewModel::markTakenAt,
            onTakeAsNeeded = viewModel::takeAsNeeded,
            onAddStock = viewModel::addStock,
            onAddMedication = onAddMedication,
            onFixExactAlarms = { SystemSettings.openExactAlarmSettings(context) },
            onFixNotifications = { SystemSettings.openNotificationSettings(context) }
        )
    )
}

@Composable
fun TodayContent(
    state: TodayUiState,
    header: HeaderProfile?,
    permissions: PermissionSnapshot,
    actions: TodayActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    var exactBannerDismissed by rememberSaveable { mutableStateOf(false) }
    var timeSheetFor by remember { mutableStateOf<TodayDose?>(null) }
    var stockSheetFor by remember { mutableStateOf<MedicationEntity?>(null) }
    val dateLabel = state.today?.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
        ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }

    Scaffold(
        containerColor = Doz.colors.bgApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ScreenHeader(stringResource(R.string.tab_today), header, subtitle = dateLabel) },
        floatingActionButton = {
            if (state.hasMedications) {
                DozExtendedFab(stringResource(R.string.add_medication), onClick = actions.onAddMedication)
            }
        }
    ) { padding ->
        if (!state.isLoading && !state.hasMedications) {
            EmptyToday(Modifier.padding(padding), actions.onAddMedication)
        } else {
            TodayList(
                state = state,
                padding = padding,
                permissions = permissions,
                actions = actions,
                is24Hour = is24Hour,
                exactBannerDismissed = exactBannerDismissed,
                onDismissExactBanner = { exactBannerDismissed = true },
                onOpenTimeSheet = { timeSheetFor = it },
                onOpenStockSheet = { stockSheetFor = it }
            )
        }
    }

    SheetHost(timeSheetFor, stockSheetFor, actions, { timeSheetFor = null }, { stockSheetFor = null })
}

@Composable
private fun TodayList(
    state: TodayUiState,
    padding: PaddingValues,
    permissions: PermissionSnapshot,
    actions: TodayActions,
    is24Hour: Boolean,
    exactBannerDismissed: Boolean,
    onDismissExactBanner: () -> Unit,
    onOpenTimeSheet: (TodayDose) -> Unit,
    onOpenStockSheet: (MedicationEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(start = Space.s16, end = Space.s16, top = Space.s8, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(Space.s12)
    ) {
        if (!permissions.exactAlarmsAllowed && !exactBannerDismissed) {
            item(key = "banner_exact") {
                InlineBanner(
                    kind = BannerKind.Warning,
                    text = stringResource(R.string.banner_exact_alarm),
                    actionLabel = stringResource(R.string.fix),
                    onAction = actions.onFixExactAlarms,
                    onDismiss = onDismissExactBanner
                )
            }
        }
        if (!permissions.notificationsAllowed) {
            item(key = "banner_notifications") {
                InlineBanner(
                    kind = BannerKind.Warning,
                    text = stringResource(R.string.banner_notifications_off),
                    actionLabel = stringResource(R.string.turn_on),
                    onAction = actions.onFixNotifications,
                    icon = Icons.Rounded.NotificationsOff
                )
            }
        }
        if (state.allDone) {
            item(key = "all_done") {
                InlineBanner(
                    kind = BannerKind.Info,
                    text = stringResource(R.string.today_all_done),
                    icon = Icons.Rounded.CheckCircle
                )
            }
        }
        if (state.segments.isNotEmpty()) {
            item(key = "progress") {
                DayProgressCard(state.segments, state.next?.let { nextLine(it, is24Hour) })
            }
        } else if (!state.isLoading) {
            item(key = "no_doses") {
                Text(
                    stringResource(R.string.no_doses_today),
                    style = Doz.type.bodyLarge,
                    color = Doz.colors.textSecondary,
                    modifier = Modifier.padding(vertical = Space.s8)
                )
            }
        }
        state.groups.forEach { group ->
            item(key = "header_${group.part}") {
                SectionHeader(stringResource(partLabel(group.part)), icon = partIcon(group.part))
            }
            items(group.doses, key = { "dose_${it.id}" }) { dose ->
                DoseCard(
                    model = dose.toCardModel(is24Hour),
                    actions = DoseCardActions(
                        onTake = { actions.onTake(dose) },
                        onUndo = { actions.onUndo(dose.id) },
                        onMarkNotTaken = { actions.onMarkSkipped(dose.id) },
                        onFixTime = { onOpenTimeSheet(dose) },
                        onUpdate = { onOpenTimeSheet(dose) },
                        onMarkSkipped = { actions.onMarkSkipped(dose.id) }
                    ),
                    modifier = Modifier.animateItem()
                )
            }
            items(group.lowStock, key = { "low_${group.part}_${it.medication.id}" }) { item ->
                LowStockEntry(item) { onOpenStockSheet(item.medication) }
            }
        }
        items(state.unplacedLowStock, key = { "low_${it.medication.id}" }) { item ->
            LowStockEntry(item) { onOpenStockSheet(item.medication) }
        }
        if (state.prn.isNotEmpty()) {
            item(key = "prn_header") { SectionHeader(stringResource(R.string.section_as_needed)) }
            items(state.prn, key = { "prn_${it.medication.id}" }) { prn ->
                PrnCard(
                    name = prn.medication.name,
                    detail = prnDetail(prn, state.today, is24Hour),
                    form = prn.medication.form,
                    onTakeNow = { actions.onTakeAsNeeded(prn.medication.id) }
                )
            }
        }
    }
}

@Composable
private fun SheetHost(
    timeSheetFor: TodayDose?,
    stockSheetFor: MedicationEntity?,
    actions: TodayActions,
    closeTimeSheet: () -> Unit,
    closeStockSheet: () -> Unit
) {
    timeSheetFor?.let { dose ->
        TakenTimeSheet(
            medicationName = dose.details.medicationName,
            initial = dose.takenTime ?: dose.plannedTime,
            onSave = { time ->
                closeTimeSheet()
                actions.onMarkTakenAt(dose, time)
            },
            onDismiss = { closeTimeSheet() },
            onSkip = if (dose.status == DoseStatus.MISSED) {
                {
                    closeTimeSheet()
                    actions.onMarkSkipped(dose.id)
                }
            } else {
                null
            }
        )
    }
    stockSheetFor?.let { medication ->
        AddStockSheet(
            medicationName = medication.name,
            unit = medication.doseUnit,
            onAdd = { amount ->
                closeStockSheet()
                actions.onAddStock(medication.id, amount)
            },
            onDismiss = { closeStockSheet() }
        )
    }
}

@Composable
private fun EmptyToday(modifier: Modifier, onAdd: () -> Unit) {
    Box(modifier.fillMaxSize().padding(Space.s24), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s16)
        ) {
            Text(
                stringResource(R.string.empty_today_title),
                style = Doz.type.titleLarge,
                color = Doz.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.empty_today_body),
                style = Doz.type.bodyLarge,
                color = Doz.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            PrimaryButton(stringResource(R.string.add_medication), onClick = onAdd)
        }
    }
}

@Composable
private fun LowStockEntry(item: LowStockItem, onAdd: () -> Unit) {
    LowStockCard(
        name = item.medication.name,
        stockLabel = stockLabel(item.medication.stockQuantity ?: 0.0, item.medication.doseUnit, item.remainingDays),
        fraction = item.fraction,
        onAddStock = onAdd
    )
}

/** "38 tablet · ~19 gün yeter", or just the amount when no estimate is possible. */
@Composable
fun stockLabel(stock: Double, unit: String, days: Int?): String {
    val amount = Formats.dose(stock, unit)
    return if (days == null) amount else stringResource(R.string.stock_with_days, amount, days)
}

@Composable
private fun nextLine(next: NextDose, is24Hour: Boolean): String {
    val time = Formats.time(next.time, is24Hour)
    return if (next.snoozed) {
        stringResource(R.string.next_dose_snoozed, next.name, time)
    } else {
        stringResource(R.string.next_dose, next.name, time)
    }
}

@Composable
private fun prnDetail(prn: PrnItem, today: LocalDate?, is24Hour: Boolean): String {
    val med = prn.medication
    val first = DoseText.strength(med.strengthAmount, med.strengthUnit)
        ?: Formats.dose(med.doseAmount, med.doseUnit)
    val last = prn.lastTakenAt ?: return first
    val local = last.atZone(ZoneId.systemDefault())
    val time = Formats.time(local.toLocalTime(), is24Hour)
    val whenText = when (local.toLocalDate()) {
        today -> stringResource(R.string.prn_last_today, time)
        today?.minusDays(1) -> stringResource(R.string.prn_last_yesterday, time)
        else -> stringResource(
            R.string.prn_last_date,
            local.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
            time
        )
    }
    return "$first${DoseText.SEPARATOR}$whenText"
}

@Composable
private fun TodayDose.toCardModel(is24Hour: Boolean): DoseCardModel {
    val d = details
    val strength = DoseText.strength(d.strengthAmount, d.strengthUnit)
    val taken = takenTime?.let {
        val formatted = Formats.time(it, is24Hour)
        if (TurkishSuffix.isTurkish()) {
            TurkishSuffix.timeLocative(formatted, it)
        } else {
            stringResource(R.string.taken_at, formatted)
        }
    }
    return DoseCardModel(
        name = d.medicationName,
        doseLine = DoseText.line(strength, d.event.quantity, d.doseUnit, d.medicationNote),
        form = d.form,
        status = status,
        plannedTime = Formats.time(plannedTime, is24Hour),
        snoozedTime = if (status == DoseStatus.SNOOZED) Formats.time(scheduledTime, is24Hour) else null,
        takenTimeLabel = taken,
        canUndo = canUndo
    )
}

fun partLabel(part: DayPart): Int = when (part) {
    DayPart.MORNING -> R.string.part_morning
    DayPart.NOON -> R.string.part_noon
    DayPart.EVENING -> R.string.part_evening
}

fun partIcon(part: DayPart) = when (part) {
    DayPart.MORNING -> DozIcons.Morning
    DayPart.NOON -> DozIcons.Noon
    DayPart.EVENING -> DozIcons.Evening
}

@DozPreviews
@Composable
private fun TodayPreview() {
    fun dose(
        id: Long,
        name: String,
        status: DoseStatus,
        hour: Int,
        form: MedicationForm = MedicationForm.TABLET
    ) = TodayDose(
        details = DoseEventDetails(
            event = DoseEventEntity(
                id = id,
                scheduleId = 1,
                medicationId = id,
                profileId = 1,
                originalScheduledAt = 0,
                scheduledAt = 0,
                status = status,
                notifiedAt = null,
                actionAt = null,
                quantity = 1.0
            ),
            medicationName = name,
            doseAmount = 1.0,
            doseUnit = "tablet",
            scheduleType = ScheduleType.DAILY,
            profileName = "Ayşe",
            form = form,
            strengthAmount = 500.0,
            strengthUnit = "mg"
        ),
        plannedTime = LocalTime.of(hour, 0),
        scheduledTime = LocalTime.of(hour, if (status == DoseStatus.SNOOZED) 10 else 0),
        takenTime = if (status == DoseStatus.TAKEN) LocalTime.of(hour, 4) else null,
        canUndo = status == DoseStatus.TAKEN
    )
    val doses = listOf(
        dose(1, "Metformin", DoseStatus.TAKEN, 8),
        dose(2, "D vitamini", DoseStatus.SNOOZED, 13, MedicationForm.DROPS),
        dose(3, "Atorvastatin", DoseStatus.PENDING, 20)
    )
    DozDefteriTheme {
        TodayContent(
            state = TodayUiState(
                isLoading = false,
                today = LocalDate.of(2026, 11, 1),
                hasMedications = true,
                groups = TodayGrouping.build(doses, emptyList()),
                segments = doses.map { it.status },
                next = TodayGrouping.next(doses)
            ),
            header = HeaderProfile("Ayşe", 0) {},
            permissions = PermissionSnapshot(notificationsAllowed = true, exactAlarmsAllowed = false),
            actions = TodayActions()
        )
    }
}
