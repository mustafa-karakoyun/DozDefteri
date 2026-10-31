@file:OptIn(ExperimentalLayoutApi::class)

package app.dozdefteri.ui.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.domain.DayStatus
import app.dozdefteri.export.PdfReport
import app.dozdefteri.ui.components.ConfirmDialog
import app.dozdefteri.ui.components.DateRangeSelectDialog
import app.dozdefteri.ui.components.DozDropdownField
import app.dozdefteri.ui.components.DozTopBar
import app.dozdefteri.ui.components.OutlinedPillButton
import app.dozdefteri.ui.components.PrimaryButton
import app.dozdefteri.ui.components.RowTrailing
import app.dozdefteri.ui.components.SelectChip
import app.dozdefteri.ui.components.SettingsRow
import app.dozdefteri.ui.components.TonalButton
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.Amber
import app.dozdefteri.ui.theme.Clay
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Sand
import app.dozdefteri.ui.theme.Teal
import app.dozdefteri.ui.theme.Space
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ExportActions(
    val onSelectProfile: (Long?) -> Unit = {},
    val onSelectRange: (RangeOption) -> Unit = {},
    val onCustomRange: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    val onSelectFormat: (ReportFormat) -> Unit = {},
    val onCreate: () -> Unit = {},
    val onBackup: () -> Unit = {},
    val onRestore: () -> Unit = {},
    val onConfirmRestore: () -> Unit = {},
    val onDismissRestore: () -> Unit = {},
    val onBackupReminder: (Boolean) -> Unit = {},
    val onBack: () -> Unit = {}
)

@Composable
fun ExportScreen(onBack: () -> Unit, viewModel: ExportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val allProfilesLabel = stringResource(R.string.all_profiles)
    LaunchedEffect(allProfilesLabel) { viewModel.setAllProfilesLabel(allProfilesLabel) }

    var legacyBackupName by rememberSaveable { mutableStateOf("") }
    val legacyBackupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_PICK_MIME)) { uri ->
            uri?.let { viewModel.backupTo(it, legacyBackupName) }
        }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(viewModel::readBackup)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExportEvent.Share -> share(context, event.uri, event.mimeType)
                is ExportEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.text))
                is ExportEvent.PickBackupLocation -> {
                    legacyBackupName = event.fileName
                    legacyBackupLauncher.launch(event.fileName)
                }
            }
        }
    }
    ExportContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = ExportActions(
            onSelectProfile = viewModel::selectProfile,
            onSelectRange = viewModel::selectRange,
            onCustomRange = viewModel::setCustomRange,
            onSelectFormat = viewModel::selectFormat,
            onCreate = viewModel::createAndShare,
            onBackup = viewModel::backupNow,
            onRestore = { restoreLauncher.launch(arrayOf("*/*")) },
            onConfirmRestore = viewModel::confirmRestore,
            onDismissRestore = viewModel::dismissRestore,
            onBackupReminder = viewModel::setBackupReminder,
            onBack = onBack
        )
    )
}

private const val BACKUP_PICK_MIME = "application/octet-stream"

private fun share(context: Context, uri: Uri, mimeType: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No app can receive the file.
    }
}

@Composable
fun ExportContent(state: ExportUiState, snackbarHostState: SnackbarHostState, actions: ExportActions) {
    Scaffold(
        containerColor = Doz.colors.bgApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { DozTopBar(stringResource(R.string.reports_title), onNavigate = actions.onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s16)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.s12)
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Doz.colors.primary)
            SectionTitle(stringResource(R.string.reports_doctor_section))
            ReportCard(state, actions)
            SectionTitle(stringResource(R.string.backup_section))
            BackupCard(state, actions)
            Box(Modifier.height(Space.s16))
        }
    }
    if (state.pendingRestore != null) {
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            text = stringResource(R.string.backup_restore_confirm_text),
            confirmLabel = stringResource(R.string.backup_restore_confirm),
            onConfirm = actions.onConfirmRestore,
            onDismiss = actions.onDismissRestore
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = Doz.type.titleMedium,
        color = Doz.colors.textSecondary,
        modifier = Modifier
            .padding(top = Space.s12)
            .semantics { heading() }
    )
}

@Composable
private fun CardSurface(content: @Composable () -> Unit) {
    Surface(
        shape = DozShapes.card,
        color = Doz.colors.surfaceDefault,
        border = BorderStroke(1.dp, Doz.colors.borderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s16)) { content() }
    }
}

@Composable
private fun ReportCard(state: ExportUiState, actions: ExportActions) {
    var pickRange by rememberSaveable { mutableStateOf(false) }
    CardSurface {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
            verticalArrangement = Arrangement.spacedBy(Space.s8)
        ) {
            listOf(
                RangeOption.LAST_MONTH to R.string.range_last_month,
                RangeOption.LAST_3_MONTHS to R.string.range_last_3_months,
                RangeOption.CUSTOM to R.string.range_custom
            ).forEach { (option, label) ->
                SelectChip(
                    text = stringResource(label),
                    selected = state.range == option,
                    onClick = { if (option == RangeOption.CUSTOM) pickRange = true else actions.onSelectRange(option) },
                    showCheck = true
                )
            }
        }
        ProfileRangeField(state, actions.onSelectProfile)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
            FormatCard(
                title = stringResource(R.string.format_pdf),
                description = stringResource(R.string.format_pdf_desc),
                selected = state.format == ReportFormat.PDF,
                onClick = { actions.onSelectFormat(ReportFormat.PDF) },
                modifier = Modifier.weight(1f)
            )
            FormatCard(
                title = stringResource(R.string.format_csv),
                description = stringResource(R.string.format_csv_desc),
                selected = state.format == ReportFormat.CSV,
                onClick = { actions.onSelectFormat(ReportFormat.CSV) },
                modifier = Modifier.weight(1f)
            )
        }
        if (state.format == ReportFormat.PDF) state.preview?.let { PdfThumbnail(it) }
        PrimaryButton(
            stringResource(
                if (state.format == ReportFormat.PDF) R.string.create_pdf_share else R.string.create_csv_share
            ),
            onClick = actions.onCreate,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (pickRange) {
        DateRangeSelectDialog(
            initialStart = state.start,
            initialEnd = state.end,
            onConfirm = { start, end ->
                pickRange = false
                actions.onCustomRange(start, end)
            },
            onDismiss = { pickRange = false }
        )
    }
}

private sealed interface ProfileChoice {
    data class One(val profile: ProfileEntity) : ProfileChoice
    data object All : ProfileChoice
}

@Composable
private fun ProfileRangeField(state: ExportUiState, onSelect: (Long?) -> Unit) {
    val allLabel = stringResource(R.string.all_profiles)
    val choices: List<ProfileChoice> = state.profiles.map { ProfileChoice.One(it) } +
        if (state.profiles.size > 1) listOf(ProfileChoice.All) else emptyList()
    val selected: ProfileChoice = if (state.allProfilesSelected) {
        ProfileChoice.All
    } else {
        choices.firstOrNull { it is ProfileChoice.One && it.profile.id == state.selectedProfile?.id }
            ?: ProfileChoice.All
    }
    DozDropdownField(
        selected = selected,
        options = choices,
        label = { choice ->
            val name = when (choice) {
                is ProfileChoice.One -> choice.profile.name
                ProfileChoice.All -> allLabel
            }
            "$name · ${state.rangeLabel}"
        },
        onSelect = { choice ->
            onSelect(
                when (choice) {
                    is ProfileChoice.One -> choice.profile.id
                    ProfileChoice.All -> null
                }
            )
        }
    )
}

@Composable
private fun FormatCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val c = Doz.colors
    Surface(
        shape = DozShapes.field,
        color = if (selected) c.primaryContainer else c.surfaceDefault,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.primary else c.borderStrong),
        modifier = modifier
            .heightIn(min = 64.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Column(Modifier.padding(Space.s12)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s4)
            ) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onPrimaryContainer)
                Text(
                    title,
                    style = Doz.type.titleMedium,
                    color = if (selected) c.onPrimaryContainer else c.textPrimary
                )
            }
            Text(
                description,
                style = Doz.type.caption,
                color = if (selected) c.onPrimaryContainer else c.textSecondary
            )
        }
    }
}

/** Miniature of the doctor report so the user sees what will be shared. */
@Composable
private fun PdfThumbnail(report: PdfReport) {
    val c = Doz.colors
    val description = stringResource(R.string.pdf_preview_a11y)
    Box(
        Modifier
            .fillMaxWidth()
            .background(c.surfaceContainerHigh, DozShapes.card)
            .padding(vertical = Space.s16)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(PAPER_RATIO)
                .background(Sand.c0)
                .padding(Space.s12),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(report.title, style = paperStyle(7, bold = true))
            report.subtitleLines.take(1).forEach { Text(it, style = paperStyle(5)) }
            Text(report.adherenceLine, style = paperStyle(6, bold = true))
            report.medicationLines.take(MAX_PREVIEW_LINES).forEach {
                Text(it, style = paperStyle(5), maxLines = 1)
            }
            if (report.days.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    report.days.takeLast(MAX_PREVIEW_DAYS).forEach { status ->
                        val color = when (status) {
                            DayStatus.ALL_TAKEN -> Teal.c600
                            DayStatus.PARTIAL -> Amber.c700
                            DayStatus.MISSED -> Clay.c700
                            DayStatus.NONE -> Sand.c200
                        }
                        Box(Modifier.weight(1f).fillMaxHeight().background(color))
                    }
                }
            }
            Text(report.footnote, style = paperStyle(4))
        }
    }
}

/** Text on the white paper preview stays dark in both themes, like the real PDF. */
private fun paperStyle(size: Int, bold: Boolean = false) = TextStyle(
    fontSize = size.sp,
    lineHeight = (size + 2).sp,
    color = Sand.c900,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
)

private const val PAPER_RATIO = 595f / 842f
private const val MAX_PREVIEW_LINES = 8
private const val MAX_PREVIEW_DAYS = 31

@Composable
private fun BackupCard(state: ExportUiState, actions: ExportActions) {
    val c = Doz.colors
    val dateTime = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.getDefault())
    CardSurface {
        Text(
            state.lastBackupAt?.let {
                stringResource(R.string.backup_last, it.atZone(ZoneId.systemDefault()).format(dateTime))
            } ?: stringResource(R.string.backup_never),
            style = Doz.type.bodyLarge,
            color = c.textPrimary
        )
        state.lastBackupLocation?.let { location ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Space.s12),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceContainer, DozShapes.small)
                    .padding(Space.s12)
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null, tint = c.textSecondary)
                Text(prettyPath(location), style = Doz.type.bodyMedium, color = c.textPrimary)
            }
        }
        TonalButton(
            stringResource(R.string.backup_create_now),
            onClick = actions.onBackup,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedPillButton(
            stringResource(R.string.backup_restore),
            onClick = actions.onRestore,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(color = c.borderDefault)
        SettingsRow(
            title = stringResource(R.string.backup_monthly_reminder),
            subtitle = state.nextReminder?.takeIf { state.backupReminderEnabled }?.let {
                stringResource(
                    R.string.backup_next_reminder,
                    it.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
                )
            },
            trailing = RowTrailing.Toggle(state.backupReminderEnabled, actions.onBackupReminder),
            horizontalPadding = 0.dp
        )
    }
}

/** "Download/DozDefteri/yedek-….ddb" → "İndirilenler / DozDefteri / yedek-….ddb". */
@Composable
private fun prettyPath(location: String): String {
    val downloads = stringResource(R.string.downloads_folder)
    return location.split('/').joinToString(" / ") { if (it == "Download") downloads else it }
}

@DozPreviews
@Composable
private fun ExportPreview() {
    DozDefteriTheme {
        ExportContent(
            state = ExportUiState(
                profiles = listOf(ProfileEntity(id = 1, name = "Ayşe", colorIndex = 0, createdAt = 0)),
                selectedProfile = ProfileEntity(id = 1, name = "Ayşe", colorIndex = 0, createdAt = 0),
                start = LocalDate.of(2026, 10, 1),
                end = LocalDate.of(2026, 10, 31),
                rangeLabel = "1–31 Ekim 2026",
                lastBackupLocation = "Download/DozDefteri/yedek-2026-10-03.ddb",
                nextReminder = LocalDate.of(2026, 11, 3),
                preview = PdfReport(
                    title = "DozDefteri · İlaç uyum özeti",
                    subtitleLines = listOf("Ayşe · 1–31 Ekim 2026"),
                    adherenceLine = "Uyum: %92 (136/148)",
                    medicationLines = listOf(
                        "Levotiroksin 50 mcg · 07:30 · 31/31",
                        "Metformin 500 mg · 08:00, 20:00 · 58/62"
                    ),
                    days = List(31) { if (it % 7 == 3) DayStatus.MISSED else DayStatus.ALL_TAKEN },
                    stripLegend = "",
                    emptyText = "",
                    footnote = "Atlanan/işaretlenmeyen dozlar kullanıcı kaydına dayanır.",
                    disclaimer = "",
                    pageFormat = "%1\$d/%2\$d"
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            actions = ExportActions()
        )
    }
}
