package app.dozdefteri.export

import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.ScheduleType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class CsvExporterTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val labels = CsvLabels(
        headers = CsvExporter.COLUMNS,
        statuses = mapOf(DoseStatus.TAKEN to "Alındı", DoseStatus.MISSED to "Kaçırıldı")
    )

    private fun millis(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun dose(id: Long, name: String, at: String, status: DoseStatus, actionAt: String? = null) =
        DoseEventDetails(
            event = DoseEventEntity(
                id = id,
                scheduleId = 1,
                medicationId = 1,
                profileId = 1,
                originalScheduledAt = millis(at),
                scheduledAt = millis(at),
                status = status,
                notifiedAt = null,
                actionAt = actionAt?.let(::millis),
                quantity = 1.5
            ),
            medicationName = name,
            doseAmount = 1.5,
            doseUnit = "tablet",
            scheduleType = ScheduleType.DAILY,
            profileName = "Ayşe"
        )

    private fun export(doses: List<DoseEventDetails>): ByteArray {
        val out = ByteArrayOutputStream()
        CsvExporter().write(doses, labels, zone, out, Locale.US)
        return out.toByteArray()
    }

    @Test
    fun startsWithUtf8Bom() {
        val bytes = export(emptyList())
        assertThat(bytes.copyOfRange(0, 3)).isEqualTo(CsvExporter.BOM)
        val text = bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
        assertThat(text).isEqualTo(CsvExporter.COLUMNS.joinToString(",") + "\r\n")
    }

    @Test
    fun quotesCommasAndQuotes_andKeepsColumnOrder() {
        val bytes = export(
            listOf(
                dose(2, "Kalsiyum \"Forte\"", "2026-11-02T20:00", DoseStatus.MISSED),
                dose(1, "Demir, 100 mg", "2026-11-02T08:00", DoseStatus.TAKEN, actionAt = "2026-11-02T08:05")
            )
        )
        val lines = bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8).split("\r\n")
        assertThat(lines[0]).isEqualTo("date,planned_time,medication,dose,unit,status,action_time,profile")
        assertThat(lines[1]).isEqualTo("2026-11-02,08:00,\"Demir, 100 mg\",1.5,tablet,Alındı,2026-11-02 08:05,Ayşe")
        assertThat(lines[2]).isEqualTo("2026-11-02,20:00,\"Kalsiyum \"\"Forte\"\"\",1.5,tablet,Kaçırıldı,,Ayşe")
    }

    @Test
    fun escape_handlesLineBreaks() {
        assertThat(CsvExporter.escape("a\nb")).isEqualTo("\"a\nb\"")
        assertThat(CsvExporter.escape("plain")).isEqualTo("plain")
    }

    @Test
    fun fileNames_areAscii() {
        assertThat(FileNames.asciiSlug("Ayşe Gül Çığ")).isEqualTo("Ayse_Gul_Cig")
        assertThat(FileNames.asciiSlug("İÖÜ")).isEqualTo("IOU")
        assertThat(FileNames.asciiSlug("???")).isEqualTo("profile")
        assertThat(FileNames.csv("Ben", java.time.LocalDate.of(2026, 11, 1), java.time.LocalDate.of(2026, 11, 30)))
            .isEqualTo("dozdefteri_Ben_2026-11-01_2026-11-30.csv")
    }
}
