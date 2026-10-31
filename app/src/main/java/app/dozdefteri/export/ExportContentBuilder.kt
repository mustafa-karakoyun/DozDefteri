package app.dozdefteri.export

import android.content.Context
import android.text.format.DateFormat
import app.dozdefteri.R
import app.dozdefteri.core.format.DoseText
import app.dozdefteri.core.format.Formats
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.repository.MedicationWithSchedule
import app.dozdefteri.domain.DayStatus
import app.dozdefteri.domain.DayStatusCalculator
import app.dozdefteri.domain.HistoryStats
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.components.statusLabelRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Resolves the localized texts of CSV and PDF exports. */
class ExportContentBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun csvLabels(): CsvLabels = CsvLabels(
        headers = listOf(
            R.string.csv_date,
            R.string.csv_planned_time,
            R.string.csv_medication,
            R.string.csv_dose,
            R.string.csv_unit,
            R.string.csv_status,
            R.string.csv_action_time,
            R.string.csv_profile
        ).map(context::getString),
        statuses = DoseStatus.entries.associateWith { context.getString(statusLabelRes(it)) }
    )

    /** "1–31 Ekim 2026" */
    fun rangeLabel(start: LocalDate, end: LocalDate): String {
        val full = DateTimeFormatter.ofPattern("d MMMM yyyy")
        return if (start.year == end.year && start.month == end.month) {
            "${start.dayOfMonth}–${end.format(full)}"
        } else {
            "${start.format(DateTimeFormatter.ofPattern("d MMMM"))} – ${end.format(full)}"
        }
    }

    @Suppress("LongParameterList")
    fun pdfReport(
        profileName: String,
        start: LocalDate,
        end: LocalDate,
        today: LocalDate,
        zone: ZoneId,
        medications: List<MedicationWithSchedule>,
        doses: List<DoseEventDetails>
    ): PdfReport {
        val is24Hour = DateFormat.is24HourFormat(context)
        val scheduled = doses.filter { it.scheduleType != ScheduleType.AS_NEEDED }
        val counts = HistoryStats.count(scheduled.map { it.event.status })
        val byMedication = doses.groupBy { it.event.medicationId }
        val lines = medications.map { item ->
            val med = item.medication
            val name = DoseText.title(med.name, DoseText.strength(med.strengthAmount, med.strengthUnit))
            val events = byMedication[med.id].orEmpty()
            if (item.schedule?.type == ScheduleType.AS_NEEDED) {
                val times = events.count { it.event.status == DoseStatus.TAKEN }
                context.getString(R.string.pdf_line_prn, name, times)
            } else {
                val medCounts = HistoryStats.count(events.map { it.event.status })
                val times = item.schedule?.times.orEmpty().joinToString(", ") { Formats.time(it, is24Hour) }
                context.getString(R.string.pdf_line, name, times, medCounts.taken, medCounts.planned)
            }
        }
        val statusesByDay = scheduled.groupBy {
            Instant.ofEpochMilli(it.event.originalScheduledAt).atZone(zone).toLocalDate()
        }
        val dayCount = ChronoUnit.DAYS.between(start, end).toInt() + 1
        val days = (0 until dayCount).map { offset ->
            val date = start.plusDays(offset.toLong())
            if (date.isAfter(today)) {
                DayStatus.NONE
            } else {
                DayStatusCalculator.calculate(statusesByDay[date].orEmpty().map { it.event.status })
            }
        }
        val percent = counts.percent?.let { context.getString(R.string.percent_value, it) }
            ?: context.getString(R.string.no_value)
        return PdfReport(
            title = context.getString(R.string.pdf_title),
            subtitleLines = listOf(
                "$profileName · ${rangeLabel(start, end)}",
                context.getString(
                    R.string.pdf_generated,
                    today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                )
            ),
            adherenceLine = context.getString(R.string.pdf_adherence_line, percent, counts.taken, counts.planned),
            medicationLines = lines,
            days = days,
            stripLegend = context.getString(R.string.pdf_strip_legend),
            emptyText = context.getString(R.string.pdf_empty),
            footnote = context.getString(R.string.pdf_footnote),
            disclaimer = context.getString(R.string.disclaimer),
            pageFormat = context.getString(R.string.pdf_page)
        )
    }
}
