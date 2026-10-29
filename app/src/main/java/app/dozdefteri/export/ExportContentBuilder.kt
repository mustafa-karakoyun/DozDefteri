package app.dozdefteri.export

import android.content.Context
import android.text.format.DateFormat
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.core.format.ScheduleFormatter
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.repository.MedicationWithSchedule
import app.dozdefteri.domain.AdherenceCalculator
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.components.statusLabelRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/** Resolves the localized texts of CSV and PDF exports. */
class ExportContentBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adherenceCalculator: AdherenceCalculator
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

    @Suppress("LongParameterList")
    fun pdfReport(
        profileName: String,
        start: LocalDate,
        end: LocalDate,
        generatedAt: Instant,
        zone: ZoneId,
        medications: List<MedicationWithSchedule>,
        doses: List<DoseEventDetails>,
        detailed: Boolean
    ): PdfReport {
        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val is24Hour = DateFormat.is24HourFormat(context)
        val scheduled = doses.filter { it.scheduleType != ScheduleType.AS_NEEDED }
        return PdfReport(
            title = context.getString(R.string.pdf_title),
            subtitleLines = listOf(
                context.getString(R.string.pdf_profile, profileName),
                context.getString(R.string.pdf_range, start.format(dateFormat), end.format(dateFormat)),
                context.getString(R.string.pdf_generated, generatedAt.atZone(zone).toLocalDate().format(dateFormat))
            ),
            medicationsTitle = context.getString(R.string.pdf_medications),
            medicationHeaders = listOf(
                context.getString(R.string.csv_medication),
                context.getString(R.string.csv_dose),
                context.getString(R.string.pdf_schedule)
            ),
            medicationRows = medications.map {
                listOf(
                    it.medication.name,
                    Formats.dose(it.medication.doseAmount, it.medication.doseUnit),
                    ScheduleFormatter.summary(context, it.schedule)
                )
            },
            dosesTitle = context.getString(R.string.pdf_doses),
            doseHeaders = listOf(
                context.getString(R.string.csv_date),
                context.getString(R.string.pdf_time),
                context.getString(R.string.csv_medication),
                context.getString(R.string.csv_status)
            ),
            doseRows = doses.sortedBy { it.event.originalScheduledAt }.map { dose ->
                val planned = Instant.ofEpochMilli(dose.event.originalScheduledAt).atZone(zone)
                listOf(
                    planned.toLocalDate().format(dateFormat),
                    Formats.time(planned.toLocalTime(), is24Hour),
                    dose.medicationName,
                    context.getString(statusLabelRes(dose.event.status))
                )
            },
            adherence = if (detailed) {
                AdherenceSection(
                    title = context.getString(R.string.pdf_adherence),
                    overallLine = context.getString(
                        R.string.pdf_overall_adherence,
                        percent(adherenceCalculator.calculate(scheduled.map { it.event }))
                    ),
                    headers = listOf(
                        context.getString(R.string.csv_medication),
                        context.getString(R.string.pdf_adherence)
                    ),
                    rows = scheduled.groupBy { it.event.medicationId }.values
                        .sortedBy { it.first().medicationName.lowercase() }
                        .map { list ->
                            listOf(
                                list.first().medicationName,
                                percent(adherenceCalculator.calculate(list.map { it.event }))
                            )
                        }
                )
            } else {
                null
            },
            emptyText = context.getString(R.string.pdf_empty),
            disclaimer = context.getString(R.string.disclaimer),
            pageFormat = context.getString(R.string.pdf_page)
        )
    }

    private fun percent(value: Int?): String =
        value?.let { context.getString(R.string.percent_value, it) } ?: context.getString(R.string.no_value)
}
