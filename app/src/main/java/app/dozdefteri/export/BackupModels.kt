package app.dozdefteri.export

import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.data.db.entity.ScheduleEntity
import app.dozdefteri.data.db.entity.StockLogEntity
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.domain.model.StockChangeReason
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

/** Backup file layout (plan 8.3). Separate DTOs keep the file format independent from the Room schema. */
@Serializable
data class BackupFile(
    val format: String,
    val schemaVersion: Int,
    val appVersionCode: Int,
    val exportedAt: String,
    val profiles: List<BackupProfile>,
    val medications: List<BackupMedication>,
    val schedules: List<BackupSchedule>,
    val doseEvents: List<BackupDoseEvent>,
    val stockLogs: List<BackupStockLog>
)

@Serializable
data class BackupProfile(
    val id: Long,
    val name: String,
    val colorIndex: Int,
    val createdAt: Long,
    val note: String? = null
)

@Serializable
data class BackupMedication(
    val id: Long,
    val profileId: Long,
    val name: String,
    val doseAmount: Double,
    val doseUnit: String,
    val form: String,
    val note: String? = null,
    val stockQuantity: Double? = null,
    val lowStockThreshold: Double? = null,
    val lowStockNotified: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val strengthAmount: Double? = null,
    val strengthUnit: String? = null,
    val lowStockDays: Int? = null
)

@Serializable
data class BackupSchedule(
    val id: Long,
    val medicationId: Long,
    val type: String,
    /** "HH:mm" values. */
    val times: List<String>,
    val daysOfWeekMask: Int,
    val intervalDays: Int,
    /** ISO dates. */
    val startDate: String,
    val endDate: String? = null
)

@Serializable
data class BackupDoseEvent(
    val id: Long,
    val scheduleId: Long,
    val medicationId: Long,
    val profileId: Long,
    val originalScheduledAt: Long,
    val scheduledAt: Long,
    val status: String,
    val notifiedAt: Long? = null,
    val actionAt: Long? = null,
    val quantity: Double
)

@Serializable
data class BackupStockLog(
    val id: Long,
    val medicationId: Long,
    val delta: Double,
    val reason: String,
    val createdAt: Long
)

/** Plain entity snapshot of the whole database. */
data class BackupData(
    val profiles: List<ProfileEntity>,
    val medications: List<MedicationEntity>,
    val schedules: List<ScheduleEntity>,
    val doseEvents: List<DoseEventEntity>,
    val stockLogs: List<StockLogEntity>
)

class InvalidBackupException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason { NOT_JSON, WRONG_FORMAT, UNSUPPORTED_VERSION, INVALID_CONTENT }
}

/** Converts between [BackupData] and the JSON backup file. Pure Kotlin. */
object BackupCodec {
    const val FORMAT = "dozdefteri-backup"
    const val SCHEMA_VERSION = 2

    /** Backups written by older app versions; their missing fields take default values. */
    private val SUPPORTED_VERSIONS = 1..SCHEMA_VERSION

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(data: BackupData, appVersionCode: Int, exportedAt: String): String =
        json.encodeToString(BackupFile.serializer(), toFile(data, appVersionCode, exportedAt))

    /** Parses and validates a backup; throws [InvalidBackupException] with the reason on any problem. */
    fun decode(text: String): BackupData {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            val looksLikeOtherJson = runCatching { json.parseToJsonElement(text) }.isSuccess
            val reason = if (looksLikeOtherJson) {
                InvalidBackupException.Reason.WRONG_FORMAT
            } else {
                InvalidBackupException.Reason.NOT_JSON
            }
            throw InvalidBackupException(reason, e)
        }
        if (file.format != FORMAT) throw InvalidBackupException(InvalidBackupException.Reason.WRONG_FORMAT)
        if (file.schemaVersion !in SUPPORTED_VERSIONS) {
            throw InvalidBackupException(InvalidBackupException.Reason.UNSUPPORTED_VERSION)
        }
        return try {
            fromFile(file).also(::checkReferences)
        } catch (e: InvalidBackupException) {
            throw e
        } catch (e: Exception) {
            throw InvalidBackupException(InvalidBackupException.Reason.INVALID_CONTENT, e)
        }
    }

    private fun checkReferences(data: BackupData) {
        val profileIds = data.profiles.map { it.id }.toSet()
        val medicationIds = data.medications.map { it.id }.toSet()
        val scheduleIds = data.schedules.map { it.id }.toSet()
        val valid = data.profiles.isNotEmpty() &&
            data.medications.all { it.profileId in profileIds } &&
            data.schedules.all { it.medicationId in medicationIds } &&
            data.doseEvents.all { it.scheduleId in scheduleIds } &&
            data.stockLogs.all { it.medicationId in medicationIds }
        if (!valid) throw InvalidBackupException(InvalidBackupException.Reason.INVALID_CONTENT)
    }

    private fun toFile(data: BackupData, appVersionCode: Int, exportedAt: String) = BackupFile(
        format = FORMAT,
        schemaVersion = SCHEMA_VERSION,
        appVersionCode = appVersionCode,
        exportedAt = exportedAt,
        profiles = data.profiles.map { BackupProfile(it.id, it.name, it.colorIndex, it.createdAt, it.note) },
        medications = data.medications.map {
            BackupMedication(
                id = it.id,
                profileId = it.profileId,
                name = it.name,
                doseAmount = it.doseAmount,
                doseUnit = it.doseUnit,
                form = it.form.name,
                note = it.note,
                stockQuantity = it.stockQuantity,
                lowStockThreshold = it.lowStockThreshold,
                lowStockNotified = it.lowStockNotified,
                isArchived = it.isArchived,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                strengthAmount = it.strengthAmount,
                strengthUnit = it.strengthUnit,
                lowStockDays = it.lowStockDays
            )
        },
        schedules = data.schedules.map {
            BackupSchedule(
                id = it.id,
                medicationId = it.medicationId,
                type = it.type.name,
                times = it.times.map { time -> time.toString() },
                daysOfWeekMask = it.daysOfWeekMask,
                intervalDays = it.intervalDays,
                startDate = it.startDate.toString(),
                endDate = it.endDate?.toString()
            )
        },
        doseEvents = data.doseEvents.map {
            BackupDoseEvent(
                id = it.id,
                scheduleId = it.scheduleId,
                medicationId = it.medicationId,
                profileId = it.profileId,
                originalScheduledAt = it.originalScheduledAt,
                scheduledAt = it.scheduledAt,
                status = it.status.name,
                notifiedAt = it.notifiedAt,
                actionAt = it.actionAt,
                quantity = it.quantity
            )
        },
        stockLogs = data.stockLogs.map {
            BackupStockLog(it.id, it.medicationId, it.delta, it.reason.name, it.createdAt)
        }
    )

    private fun fromFile(file: BackupFile) = BackupData(
        profiles = file.profiles.map { ProfileEntity(it.id, it.name, it.colorIndex, it.createdAt, it.note) },
        medications = file.medications.map {
            MedicationEntity(
                id = it.id,
                profileId = it.profileId,
                name = it.name,
                doseAmount = it.doseAmount,
                doseUnit = it.doseUnit,
                form = MedicationForm.valueOf(it.form),
                note = it.note,
                stockQuantity = it.stockQuantity,
                lowStockThreshold = it.lowStockThreshold,
                lowStockNotified = it.lowStockNotified,
                isArchived = it.isArchived,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                strengthAmount = it.strengthAmount,
                strengthUnit = it.strengthUnit,
                lowStockDays = it.lowStockDays
            )
        },
        schedules = file.schedules.map {
            ScheduleEntity(
                id = it.id,
                medicationId = it.medicationId,
                type = ScheduleType.valueOf(it.type),
                times = it.times.map(LocalTime::parse).distinct().sorted(),
                daysOfWeekMask = it.daysOfWeekMask,
                intervalDays = it.intervalDays,
                startDate = LocalDate.parse(it.startDate),
                endDate = it.endDate?.let(LocalDate::parse)
            )
        },
        doseEvents = file.doseEvents.map {
            DoseEventEntity(
                id = it.id,
                scheduleId = it.scheduleId,
                medicationId = it.medicationId,
                profileId = it.profileId,
                originalScheduledAt = it.originalScheduledAt,
                scheduledAt = it.scheduledAt,
                status = DoseStatus.valueOf(it.status),
                notifiedAt = it.notifiedAt,
                actionAt = it.actionAt,
                quantity = it.quantity
            )
        },
        stockLogs = file.stockLogs.map {
            StockLogEntity(it.id, it.medicationId, it.delta, StockChangeReason.valueOf(it.reason), it.createdAt)
        }
    )
}
