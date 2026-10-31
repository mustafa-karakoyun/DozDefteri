package app.dozdefteri.ui.export

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.R
import app.dozdefteri.alarm.DoseNotifier
import app.dozdefteri.alarm.DoseSync
import app.dozdefteri.data.db.dao.DoseEventDetails
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
import app.dozdefteri.export.PdfReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

enum class RangeOption(val days: Long) { LAST_MONTH(30), LAST_3_MONTHS(90), CUSTOM(0) }

enum class ReportFormat { PDF, CSV }

data class ReportSelection(
    /** Selected profile; null means the active profile unless [allProfiles]. */
    val profileId: Long? = null,
    val allProfiles: Boolean = false,
    val range: RangeOption = RangeOption.LAST_MONTH,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
    val format: ReportFormat = ReportFormat.PDF,
    val busy: Boolean = false,
    val pendingRestore: BackupData? = null
)

data class ExportUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val selectedProfile: ProfileEntity? = null,
    val allProfilesSelected: Boolean = false,
    val range: RangeOption = RangeOption.LAST_MONTH,
    val start: LocalDate,
    val end: LocalDate,
    val rangeLabel: String = "",
    val format: ReportFormat = ReportFormat.PDF,
    /** Live content of the PDF, used for the thumbnail preview. */
    val preview: PdfReport? = null,
    val busy: Boolean = false,
    val pendingRestore: BackupData? = null,
    val lastBackupAt: Instant? = null,
    val lastBackupLocation: String? = null,
    val backupReminderEnabled: Boolean = true,
    val nextReminder: LocalDate? = null
)

sealed interface ExportEvent {
    data class Share(val uri: Uri, val mimeType: String) : ExportEvent
    data class Message(@StringRes val text: Int) : ExportEvent

    /** Below Android 10 the backup location is picked by the user. */
    data class PickBackupLocation(val fileName: String) : ExportEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val clock: Clock
) : ViewModel() {

    private val selection = MutableStateFlow(ReportSelection())
    private val _events = Channel<ExportEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val baseState = combine(
        profileRepository.profiles,
        profileRepository.activeProfile,
        settingsRepository.settings,
        selection
    ) { profiles, active, settings, sel ->
        val all = sel.allProfiles && profiles.size > 1
        val (start, end) = range(sel)
        val lastBackup = settings.lastBackupAt.takeIf { it > 0 }?.let(Instant::ofEpochMilli)
        val reminderBase = lastBackup
            ?: Instant.ofEpochMilli(settings.lastBackupReminderAt.takeIf { it > 0 } ?: clock.millis())
        ExportUiState(
            profiles = profiles,
            selectedProfile = if (all) null else profiles.firstOrNull { it.id == sel.profileId } ?: active,
            allProfilesSelected = all,
            range = sel.range,
            start = start,
            end = end,
            rangeLabel = contentBuilder.rangeLabel(start, end),
            format = sel.format,
            busy = sel.busy,
            pendingRestore = sel.pendingRestore,
            lastBackupAt = lastBackup,
            lastBackupLocation = settings.lastBackupLocation,
            backupReminderEnabled = settings.backupReminderEnabled,
            nextReminder = reminderBase.plus(BACKUP_INTERVAL).atZone(clock.zone).toLocalDate()
        )
    }

    val uiState: StateFlow<ExportUiState> = baseState
        .flatMapLatest { state ->
            val profileId = profileFilter(state)
            doseRepository.observeRange(profileId, startInstant(state), endInstant(state)).map { doses ->
                state.copy(preview = buildReport(state, doses))
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ExportUiState(start = today().minusDays(RangeOption.LAST_MONTH.days - 1), end = today())
        )

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun range(sel: ReportSelection): Pair<LocalDate, LocalDate> {
        val today = today()
        return if (sel.range == RangeOption.CUSTOM) {
            val start = sel.customStart ?: today.minusDays(RangeOption.LAST_MONTH.days - 1)
            val end = sel.customEnd ?: today
            if (end.isBefore(start)) end to start else start to end
        } else {
            today.minusDays(sel.range.days - 1) to today
        }
    }

    private suspend fun buildReport(state: ExportUiState, doses: List<DoseEventDetails>) =
        contentBuilder.pdfReport(
            profileName = profileLabel(state),
            start = state.start,
            end = state.end,
            today = today(),
            zone = clock.zone,
            medications = medicationRepository.getActiveWithSchedules(profileFilter(state)),
            doses = doses
        )

    private var allProfilesLabel: String = ""

    /** Localized "Tüm profiller", provided by the screen. */
    fun setAllProfilesLabel(label: String) {
        allProfilesLabel = label
    }

    private fun profileLabel(state: ExportUiState): String =
        if (state.allProfilesSelected) allProfilesLabel else state.selectedProfile?.name.orEmpty()

    /** [profileId] null selects "All profiles". */
    fun selectProfile(profileId: Long?) =
        selection.update { it.copy(profileId = profileId, allProfiles = profileId == null) }

    fun selectRange(option: RangeOption) = selection.update { it.copy(range = option) }

    fun setCustomRange(start: LocalDate, end: LocalDate) =
        selection.update { it.copy(range = RangeOption.CUSTOM, customStart = start, customEnd = end) }

    fun selectFormat(format: ReportFormat) = selection.update { it.copy(format = format) }

    /** "PDF oluştur ve paylaş" / "CSV oluştur ve paylaş". */
    fun createAndShare() = runBusy {
        val state = uiState.value
        val doses = doseRepository.getRange(profileFilter(state), startInstant(state), endInstant(state))
        val slug = if (state.allProfilesSelected) ALL_PROFILES_SLUG else state.selectedProfile?.name.orEmpty()
        if (state.format == ReportFormat.PDF) {
            val report = buildReport(state, doses)
            val uri = documentIo.writeShareable(FileNames.pdf(slug, state.start, state.end)) {
                pdfExporter.write(report, it)
            }
            _events.send(ExportEvent.Share(uri, MIME_PDF))
        } else {
            val labels = contentBuilder.csvLabels()
            val uri = documentIo.writeShareable(FileNames.csv(slug, state.start, state.end)) {
                csvExporter.write(doses, labels, clock.zone, it)
            }
            _events.send(ExportEvent.Share(uri, MIME_CSV))
        }
    }

    /** "Şimdi yedek oluştur": straight into Downloads/DozDefteri, or asks for a location on Android 8–9. */
    fun backupNow() = runBusy {
        val fileName = FileNames.backup(today())
        val bytes = backupManager.exportJson().toByteArray(Charsets.UTF_8)
        val saved = documentIo.saveToDownloads(fileName, bytes)
        if (saved == null) {
            _events.send(ExportEvent.PickBackupLocation(fileName))
        } else {
            settingsRepository.setLastBackup(clock.millis(), saved.displayPath)
            _events.send(ExportEvent.Message(R.string.backup_saved))
        }
    }

    /** Android 8–9 path: the user picked [uri] for the backup file. */
    fun backupTo(uri: Uri, displayName: String) = runBusy {
        val json = backupManager.exportJson()
        documentIo.write(uri) { it.write(json.toByteArray(Charsets.UTF_8)) }
        settingsRepository.setLastBackup(clock.millis(), displayName)
        _events.send(ExportEvent.Message(R.string.backup_saved))
    }

    fun setBackupReminder(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBackupReminderEnabled(enabled) }
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
        private const val ALL_PROFILES_SLUG = "all"
        private val BACKUP_INTERVAL: Duration = Duration.ofDays(30)
    }
}
