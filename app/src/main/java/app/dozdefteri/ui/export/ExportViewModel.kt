package app.dozdefteri.ui.export

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.R
import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.billing.ProStatus
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.prefs.SettingsRepository
import app.dozdefteri.data.repository.DoseRepository
import app.dozdefteri.data.repository.MedicationRepository
import app.dozdefteri.data.repository.ProfileRepository
import app.dozdefteri.export.BackupData
import app.dozdefteri.export.BackupManager
import app.dozdefteri.export.CsvExporter
import app.dozdefteri.export.DocumentIo
import app.dozdefteri.export.ExportContentBuilder
import app.dozdefteri.export.FileNames
import app.dozdefteri.export.InvalidBackupException
import app.dozdefteri.export.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class RangeOption(val days: Long) { LAST_7(7), LAST_30(30), LAST_90(90), CUSTOM(0) }

data class ExportSelection(
    /** null means the active profile, unless [allProfiles] is set. */
    val profileId: Long? = null,
    /** "All profiles" was picked; only honoured with Pro. */
    val allProfiles: Boolean = false,
    val range: RangeOption = RangeOption.LAST_30,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
    val busy: Boolean = false,
    /** Parsed backup waiting for the user's confirmation. */
    val pendingRestore: BackupData? = null
)

data class ExportUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val selectedProfile: ProfileEntity? = null,
    val allProfilesSelected: Boolean = false,
    val isPro: Boolean = false,
    val range: RangeOption = RangeOption.LAST_30,
    val start: LocalDate,
    val end: LocalDate,
    val busy: Boolean = false,
    val pendingRestore: BackupData? = null
)

sealed interface ExportEvent {
    data class Saved(val uri: Uri, val mimeType: String) : ExportEvent
    data class Message(@StringRes val text: Int) : ExportEvent
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val medicationRepository: MedicationRepository,
    private val doseRepository: DoseRepository,
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val csvExporter: CsvExporter,
    private val pdfExporter: PdfExporter,
    private val contentBuilder: ExportContentBuilder,
    private val documentIo: DocumentIo,
    private val doseSync: DoseSync,
    private val notifier: DoseNotifier,
    proStatus: ProStatus,
    private val clock: Clock
) : ViewModel() {

    private val selection = MutableStateFlow(ExportSelection())
    private val _events = Channel<ExportEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<ExportUiState> = combine(
        profileRepository.profiles,
        profileRepository.activeProfile,
        proStatus.isPro,
        selection
    ) { profiles, active, isPro, sel ->
        val all = isPro && sel.allProfiles
        val chosen = profiles.firstOrNull { it.id == sel.profileId } ?: active
        val (start, end) = range(sel)
        ExportUiState(
            profiles = profiles,
            selectedProfile = if (all) null else chosen,
            allProfilesSelected = all,
            isPro = isPro,
            range = sel.range,
            start = start,
            end = end,
            busy = sel.busy,
            pendingRestore = sel.pendingRestore
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ExportUiState(start = today().minusDays(RangeOption.LAST_30.days - 1), end = today())
    )

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun range(sel: ExportSelection): Pair<LocalDate, LocalDate> {
        val today = today()
        return if (sel.range == RangeOption.CUSTOM) {
            val start = sel.customStart ?: today.minusDays(RangeOption.LAST_30.days - 1)
            val end = sel.customEnd ?: today
            if (end.isBefore(start)) end to start else start to end
        } else {
            today.minusDays(sel.range.days - 1) to today
        }
    }

    /** [profileId] null selects "All profiles". */
    fun selectProfile(profileId: Long?) = selection.update { it.copy(profileId = profileId, allProfiles = profileId == null) }

    fun selectRange(option: RangeOption) = selection.update { it.copy(range = option) }

    fun setCustomStart(date: LocalDate) = selection.update { it.copy(range = RangeOption.CUSTOM, customStart = date) }

    fun setCustomEnd(date: LocalDate) = selection.update { it.copy(range = RangeOption.CUSTOM, customEnd = date) }

    private fun exportProfileName(state: ExportUiState): String =
        if (state.allProfilesSelected) ALL_PROFILES_SLUG else state.selectedProfile?.name.orEmpty()

    fun csvFileName(): String = uiState.value.let { FileNames.csv(exportProfileName(it), it.start, it.end) }

    fun pdfFileName(): String = uiState.value.let { FileNames.pdf(exportProfileName(it), it.start, it.end) }

    fun backupFileName(): String = FileNames.backup(today())

    fun exportCsv(uri: Uri) = runBusy {
        val state = uiState.value
        val doses = doseRepository.getRange(profileFilter(state), startInstant(state), endInstant(state))
        val labels = contentBuilder.csvLabels()
        documentIo.write(uri) { csvExporter.write(doses, labels, clock.zone, it) }
        _events.send(ExportEvent.Saved(uri, MIME_CSV))
    }

    fun exportPdf(uri: Uri, allProfilesLabel: String) = runBusy {
        val state = uiState.value
        val profileId = profileFilter(state)
        val doses = doseRepository.getRange(profileId, startInstant(state), endInstant(state))
        val report = contentBuilder.pdfReport(
            profileName = if (state.allProfilesSelected) allProfilesLabel else state.selectedProfile?.name.orEmpty(),
            start = state.start,
            end = state.end,
            generatedAt = clock.instant(),
            zone = clock.zone,
            medications = medicationRepository.getActiveWithSchedules(profileId),
            doses = doses,
            detailed = state.isPro
        )
        documentIo.write(uri) { pdfExporter.write(report, it) }
        _events.send(ExportEvent.Saved(uri, MIME_PDF))
    }

    fun backup(uri: Uri) = runBusy {
        val json = backupManager.exportJson()
        documentIo.write(uri) { it.write(json.toByteArray(Charsets.UTF_8)) }
        settingsRepository.setLastBackupReminderAt(clock.millis())
        _events.send(ExportEvent.Saved(uri, MIME_JSON))
    }

    /** Reads and validates a backup file; on success asks the user to confirm before anything is replaced. */
    fun readBackup(uri: Uri) = runBusy {
        val data = try {
            backupManager.parse(documentIo.readText(uri))
        } catch (e: InvalidBackupException) {
            _events.send(
                ExportEvent.Message(
                    if (e.reason == InvalidBackupException.Reason.UNSUPPORTED_VERSION) {
                        R.string.backup_error_version
                    } else {
                        R.string.backup_error_invalid
                    }
                )
            )
            return@runBusy
        }
        selection.update { it.copy(pendingRestore = data) }
    }

    fun dismissRestore() = selection.update { it.copy(pendingRestore = null) }

    fun confirmRestore() = runBusy {
        val data = selection.value.pendingRestore ?: return@runBusy
        selection.update { it.copy(pendingRestore = null) }
        backupManager.replaceAll(data)
        data.profiles.minByOrNull { it.createdAt }?.let { profileRepository.setActive(it.id) }
        notifier.cancelAll()
        doseSync.run("restore")
        _events.send(ExportEvent.Message(R.string.backup_restored))
    }

    private fun profileFilter(state: ExportUiState): Long? =
        if (state.allProfilesSelected) null else state.selectedProfile?.id

    private fun startInstant(state: ExportUiState) = state.start.atStartOfDay(clock.zone).toInstant()

    private fun endInstant(state: ExportUiState) = state.end.plusDays(1).atStartOfDay(clock.zone).toInstant()

    private fun runBusy(block: suspend () -> Unit) {
        if (selection.value.busy) return
        selection.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(ExportEvent.Message(R.string.export_failed))
            } finally {
                selection.update { it.copy(busy = false) }
            }
        }
    }

    companion object {
        const val MIME_CSV = "text/csv"
        const val MIME_PDF = "application/pdf"
        const val MIME_JSON = "application/json"
        private const val ALL_PROFILES_SLUG = "all"
    }
}
