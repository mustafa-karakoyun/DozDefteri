package app.dozdefteri.export

import app.dozdefteri.core.format.Formats
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.domain.model.DoseStatus
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** Localized texts for the CSV; the header list must follow [CsvExporter.COLUMNS]. */
data class CsvLabels(
    val headers: List<String>,
    val statuses: Map<DoseStatus, String>
)

/**
 * Writes dose history as RFC 4180 CSV: UTF-8 with BOM (so Excel shows Turkish characters), comma separator,
 * CRLF line endings, fields quoted when they contain a comma, quote or line break. Pure Kotlin.
 */
class CsvExporter @Inject constructor() {

    fun write(
        doses: List<DoseEventDetails>,
        labels: CsvLabels,
        zone: ZoneId,
        output: OutputStream,
        locale: Locale = Locale.getDefault()
    ) {
        require(labels.headers.size == COLUMNS.size) { "Expected ${COLUMNS.size} headers" }
        output.write(BOM)
        val writer = output.bufferedWriter(Charsets.UTF_8)
        writer.write(labels.headers.joinToString(",") { escape(it) })
        writer.write(LINE_END)
        doses.sortedWith(compareBy({ it.event.originalScheduledAt }, { it.event.id })).forEach { dose ->
            val planned = Instant.ofEpochMilli(dose.event.originalScheduledAt).atZone(zone)
            val action = dose.event.actionAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
            val fields = listOf(
                planned.format(DATE),
                planned.format(TIME),
                dose.medicationName,
                Formats.amount(dose.event.quantity, locale),
                dose.doseUnit,
                labels.statuses[dose.event.status] ?: dose.event.status.name,
                action?.format(DATE_TIME).orEmpty(),
                dose.profileName
            )
            writer.write(fields.joinToString(",") { escape(it) })
            writer.write(LINE_END)
        }
        writer.flush()
    }

    companion object {
        val COLUMNS = listOf("date", "planned_time", "medication", "dose", "unit", "status", "action_time", "profile")
        val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        private const val LINE_END = "\r\n"
        private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val TIME = DateTimeFormatter.ofPattern("HH:mm")
        private val DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        fun escape(value: String): String =
            if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else {
                value
            }
    }
}
