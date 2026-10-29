package app.dozdefteri.export

/** Fully localized content of a PDF report; [PdfExporter] only lays it out. */
data class PdfReport(
    val title: String,
    /** Lines under the title: profile, date range, generation date. */
    val subtitleLines: List<String>,
    val medicationsTitle: String,
    val medicationHeaders: List<String>,
    val medicationRows: List<List<String>>,
    val dosesTitle: String,
    val doseHeaders: List<String>,
    val doseRows: List<List<String>>,
    /** Detailed (Pro) template only; null for the simple template. */
    val adherence: AdherenceSection?,
    val emptyText: String,
    val disclaimer: String,
    /** Format with two %d placeholders: current page, total pages. */
    val pageFormat: String
)

data class AdherenceSection(
    val title: String,
    val overallLine: String,
    val headers: List<String>,
    val rows: List<List<String>>
)
