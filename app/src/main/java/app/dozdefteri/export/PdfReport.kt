package app.dozdefteri.export

import app.dozdefteri.domain.DayStatus

/** Fully localized content of the one-page doctor report; [PdfExporter] only lays it out. */
data class PdfReport(
    /** "DozDefteri · İlaç uyum özeti" */
    val title: String,
    /** "Ayşe · 1–31 Ekim 2026" and the generation date. */
    val subtitleLines: List<String>,
    /** "Uyum: %92 (136/148)" */
    val adherenceLine: String,
    /** "Levotiroksin 50 mcg · 07:30 · 31/31", "Parasetamol 500 mg · gerektiğinde · 4 kez" */
    val medicationLines: List<String>,
    /** One entry per day of the range, drawn as a colored strip. */
    val days: List<DayStatus>,
    /** Legend under the strip. */
    val stripLegend: String,
    val emptyText: String,
    /** "Atlanan/işaretlenmeyen dozlar kullanıcı kaydına dayanır." */
    val footnote: String,
    val disclaimer: String,
    /** Format with two %d placeholders: current page, total pages. */
    val pageFormat: String
)
