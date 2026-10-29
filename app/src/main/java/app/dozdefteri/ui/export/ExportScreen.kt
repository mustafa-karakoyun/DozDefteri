@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.dozdefteri.ui.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.ui.components.ConfirmDialog
import app.dozdefteri.ui.components.DateSelectDialog
import app.dozdefteri.ui.theme.DozDefteriTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class ExportActions(
    val onSelectProfile: (Long?) -> Unit = {},
    val onSelectRange: (RangeOption) -> Unit = {},
    val onCustomStart: (LocalDate) -> Unit = {},
    val onCustomEnd: (LocalDate) -> Unit = {},
    val onCsv: () -> Unit = {},
    val onPdf: () -> Unit = {},
    val onBackup: () -> Unit = {},
    val onRestore: () -> Unit = {},
    val onConfirmRestore: () -> Unit = {},
    val onDismissRestore: () -> Unit = {},
    val onOpenPro: () -> Unit = {},
    val onBack: () -> Unit = {}
)

@Composable
fun ExportScreen(onBack: () -> Unit, onOpenPro: () -> Unit, viewModel: ExportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val allProfilesLabel = stringResource(R.string.all_profiles)

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportViewModel.MIME_CSV)) {
        it?.let(viewModel::exportCsv)
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportViewModel.MIME_PDF)) {
        it?.let { uri -> viewModel.exportPdf(uri, allProfilesLabel) }
    }
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportViewModel.MIME_JSON)) {
            it?.let(viewModel::backup)
        }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(viewModel::readBackup)
    }

    val savedText = stringResource(R.string.file_saved)
    val shareText = stringResource(R.string.share)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExportEvent.Saved -> {
                    val result = snackbarHostState.showSnackbar(
                        message = savedText,
                        actionLabel = shareText,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) share(context, event.uri, event.mimeType)
                }
                is ExportEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.text))
            }
        }
    }

    ExportContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = ExportActions(
            onSelectProfile = viewModel::selectProfile,
            onSelectRange = viewModel::selectRange,
            onCustomStart = viewModel::setCustomStart,
            onCustomEnd = viewModel::setCustomEnd,
            onCsv = { csvLauncher.launch(viewModel.csvFileName()) },
            onPdf = { pdfLauncher.launch(viewModel.pdfFileName()) },
            onBackup = { backupLauncher.launch(viewModel.backupFileName()) },
            onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            onConfirmRestore = viewModel::confirmRestore,
            onDismissRestore = viewModel::dismissRestore,
            onOpenPro = onOpenPro,
            onBack = onBack
        )
    )
}

private fun share(context: Context, uri: Uri, mimeType: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No app can receive the file; the file itself is already saved.
    }
}

@Composable
fun ExportContent(state: ExportUiState, snackbarHostState: SnackbarHostState, actions: ExportActions) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.profiles.size > 1) ProfileChooser(state, actions)
            RangeChooser(state, actions)
            ActionButton(Icons.Filled.Description, stringResource(R.string.export_csv), !state.busy, actions.onCsv)
            ActionButton(Icons.Filled.PictureAsPdf, stringResource(R.string.export_pdf), !state.busy, actions.onPdf)
            Text(
                stringResource(if (state.isPro) R.string.export_pdf_detailed_hint else R.string.export_pdf_simple_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            SectionTitle(stringResource(R.string.backup_section))
            Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodyMedium)
            ActionButton(Icons.Filled.Backup, stringResource(R.string.backup_create), !state.busy, actions.onBackup)
            ActionButton(Icons.Filled.Restore, stringResource(R.string.backup_restore), !state.busy, actions.onRestore, outlined = true)
        }
    }

    if (state.pendingRestore != null) {
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            text = stringResource(R.string.backup_restore_confirm_text),
            confirmLabel = stringResource(R.string.backup_restore),
            onConfirm = actions.onConfirmRestore,
            onDismiss = actions.onDismissRestore
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun ProfileChooser(state: ExportUiState, actions: ExportActions) {
    SectionTitle(stringResource(R.string.export_profile))
    Column(Modifier.selectableGroup()) {
        state.profiles.forEach { profile ->
            ChoiceRow(
                label = profile.name,
                selected = !state.allProfilesSelected && state.selectedProfile?.id == profile.id,
                onClick = { actions.onSelectProfile(profile.id) }
            )
        }
        ChoiceRow(
            label = stringResource(R.string.all_profiles),
            selected = state.allProfilesSelected,
            locked = !state.isPro,
            onClick = { if (state.isPro) actions.onSelectProfile(null) else actions.onOpenPro() }
        )
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit, locked: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
    ) {
        RadioButton(selected = selected, onClick = null, enabled = !locked)
        Text(label, modifier = Modifier.padding(start = 12.dp).weight(1f))
        if (locked) {
            Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.pro_required))
        }
    }
}

@Composable
private fun RangeChooser(state: ExportUiState, actions: ExportActions) {
    var pickStart by rememberSaveable { mutableStateOf(false) }
    var pickEnd by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    SectionTitle(stringResource(R.string.export_range))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RangeOption.entries.forEach { option ->
            FilterChip(
                selected = state.range == option,
                onClick = { actions.onSelectRange(option) },
                label = { Text(stringResource(rangeLabel(option))) }
            )
        }
    }
    if (state.range == RangeOption.CUSTOM) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(state.start.format(formatter))
            }
            OutlinedButton(onClick = { pickEnd = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(state.end.format(formatter))
            }
        }
    } else {
        Text(
            stringResource(R.string.export_range_value, state.start.format(formatter), state.end.format(formatter)),
            style = MaterialTheme.typography.bodyMedium
        )
    }
    if (pickStart) {
        DateSelectDialog(
            initial = state.start,
            onConfirm = {
                pickStart = false
                actions.onCustomStart(it)
            },
            onDismiss = { pickStart = false }
        )
    }
    if (pickEnd) {
        DateSelectDialog(
            initial = state.end,
            onConfirm = {
                pickEnd = false
                actions.onCustomEnd(it)
            },
            onDismiss = { pickEnd = false }
        )
    }
}

private fun rangeLabel(option: RangeOption): Int = when (option) {
    RangeOption.LAST_7 -> R.string.range_7
    RangeOption.LAST_30 -> R.string.range_30
    RangeOption.LAST_90 -> R.string.range_90
    RangeOption.CUSTOM -> R.string.range_custom
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
    if (outlined) {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            content()
        }
    } else {
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun ExportPreview() {
    DozDefteriTheme {
        ExportContent(
            state = ExportUiState(
                profiles = listOf(
                    ProfileEntity(id = 1, name = "Me", colorIndex = 0, createdAt = 0),
                    ProfileEntity(id = 2, name = "Mom", colorIndex = 1, createdAt = 0)
                ),
                selectedProfile = ProfileEntity(id = 1, name = "Me", colorIndex = 0, createdAt = 0),
                start = LocalDate.of(2026, 10, 1),
                end = LocalDate.of(2026, 10, 30)
            ),
            snackbarHostState = remember { SnackbarHostState() },
            actions = ExportActions()
        )
    }
}
