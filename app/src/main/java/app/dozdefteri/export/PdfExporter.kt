package app.dozdefteri.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import java.io.OutputStream
import javax.inject.Inject

/**
 * Renders a [PdfReport] on A4 pages (595 × 842 pt, 40 pt margins) with [PdfDocument]. Tables continue on new
 * pages with their header row repeated; every page carries the disclaimer and an "n/total" page number.
 */
class PdfExporter @Inject constructor() {

    private val titlePaint = textPaint(TITLE_SIZE, bold = true)
    private val bodyPaint = textPaint(BODY_SIZE, bold = false)
    private val boldPaint = textPaint(BODY_SIZE, bold = true)
    private val footerPaint = textPaint(FOOTER_SIZE, bold = false)
    private val linePaint = Paint().apply { strokeWidth = 0.5f; color = 0xFFBDBDBD.toInt() }

    fun write(report: PdfReport, output: OutputStream) {
        val pages = paginate(buildItems(report))
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, items ->
                val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(info)
                val canvas = page.canvas
                var y = MARGIN.toFloat()
                items.forEach { item ->
                    draw(canvas, item, y)
                    y += item.height
                }
                drawFooter(canvas, report, index + 1, pages.size)
                document.finishPage(page)
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private sealed interface Item {
        val height: Float

        data class Text(val text: String, val paint: TextPaint, override val height: Float) : Item
        data class Spacer(override val height: Float) : Item
        data class Row(
            val cells: List<String>,
            val widths: List<Float>,
            val header: Boolean,
            /** The table this row belongs to; rows of one table share the same header row object. */
            val tableHeader: Row?
        ) : Item {
            override val height: Float get() = ROW_HEIGHT
        }
    }

    private fun buildItems(report: PdfReport): List<Item> = buildList {
        add(Item.Text(report.title, titlePaint, TITLE_SIZE + 8f))
        report.subtitleLines.forEach { line -> addAll(wrap(line, bodyPaint)) }
        add(Item.Spacer(SECTION_GAP))

        report.adherence?.let { section ->
            add(Item.Text(section.title, boldPaint, BODY_SIZE + 8f))
            addAll(wrap(section.overallLine, bodyPaint))
            addTable(section.headers, section.rows, listOf(0.7f, 0.3f), report.emptyText)
            add(Item.Spacer(SECTION_GAP))
        }

        add(Item.Text(report.medicationsTitle, boldPaint, BODY_SIZE + 8f))
        addTable(report.medicationHeaders, report.medicationRows, listOf(0.35f, 0.2f, 0.45f), report.emptyText)
        add(Item.Spacer(SECTION_GAP))

        add(Item.Text(report.dosesTitle, boldPaint, BODY_SIZE + 8f))
        addTable(report.doseHeaders, report.doseRows, listOf(0.2f, 0.13f, 0.42f, 0.25f), report.emptyText)
    }

    private fun MutableList<Item>.addTable(
        headers: List<String>,
        rows: List<List<String>>,
        fractions: List<Float>,
        emptyText: String
    ) {
        val widths = fractions.map { it * CONTENT_WIDTH }
        val header = Item.Row(headers, widths, header = true, tableHeader = null)
        add(header)
        if (rows.isEmpty()) {
            addAll(wrap(emptyText, bodyPaint))
        } else {
            rows.forEach { add(Item.Row(it, widths, header = false, tableHeader = header)) }
        }
    }

    /** Splits items into pages; a table row starting a new page is preceded by its table's header row. */
    private fun paginate(items: List<Item>): List<List<Item>> {
        val pages = mutableListOf<MutableList<Item>>(mutableListOf())
        var used = 0f
        items.forEachIndexed { index, item ->
            val next = items.getOrNull(index + 1)
            // Keep a header row together with the row after it.
            val needed = if (item is Item.Row && item.header && next != null) item.height + next.height else item.height
            if (used + needed > BODY_HEIGHT && pages.last().isNotEmpty()) {
                pages += mutableListOf<Item>()
                used = 0f
                if (item is Item.Spacer) return@forEachIndexed
                if (item is Item.Row && !item.header && item.tableHeader != null) {
                    pages.last() += item.tableHeader
                    used += item.tableHeader.height
                }
            }
            pages.last() += item
            used += item.height
        }
        return pages
    }

    private fun draw(canvas: Canvas, item: Item, top: Float) {
        when (item) {
            is Item.Spacer -> Unit
            is Item.Text -> canvas.drawText(item.text, MARGIN.toFloat(), top + item.paint.textSize, item.paint)
            is Item.Row -> {
                val paint = if (item.header) boldPaint else bodyPaint
                var x = MARGIN.toFloat()
                item.cells.forEachIndexed { i, cell ->
                    val width = item.widths.getOrElse(i) { 0f }
                    val text = TextUtils.ellipsize(cell, paint, width - CELL_PADDING, TextUtils.TruncateAt.END)
                    canvas.drawText(text.toString(), x, top + BODY_SIZE + 2f, paint)
                    x += width
                }
                canvas.drawLine(
                    MARGIN.toFloat(),
                    top + ROW_HEIGHT - 2f,
                    (MARGIN + CONTENT_WIDTH),
                    top + ROW_HEIGHT - 2f,
                    linePaint
                )
            }
        }
    }

    private fun drawFooter(canvas: Canvas, report: PdfReport, page: Int, total: Int) {
        val lines = wrapLines(report.disclaimer, footerPaint, CONTENT_WIDTH * 0.8f)
        var y = PAGE_HEIGHT - MARGIN - (lines.size - 1) * (FOOTER_SIZE + 2f)
        lines.forEach { line ->
            canvas.drawText(line, MARGIN.toFloat(), y, footerPaint)
            y += FOOTER_SIZE + 2f
        }
        val pageText = String.format(report.pageFormat, page, total)
        canvas.drawText(
            pageText,
            MARGIN + CONTENT_WIDTH - footerPaint.measureText(pageText),
            (PAGE_HEIGHT - MARGIN).toFloat(),
            footerPaint
        )
    }

    private fun wrap(text: String, paint: TextPaint): List<Item> =
        wrapLines(text, paint, CONTENT_WIDTH).map { Item.Text(it, paint, paint.textSize + 4f) }

    private fun wrapLines(text: String, paint: TextPaint, width: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        text.split(' ').forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= width || current.isEmpty()) {
                current = StringBuilder(candidate)
            } else {
                lines += current.toString()
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    private fun textPaint(size: Float, bold: Boolean) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        color = 0xFF000000.toInt()
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40
        private const val TITLE_SIZE = 16f
        private const val BODY_SIZE = 10f
        private const val FOOTER_SIZE = 7f
        private const val ROW_HEIGHT = 16f
        private const val SECTION_GAP = 12f
        private const val CELL_PADDING = 6f
        private const val FOOTER_HEIGHT = 40f
        private const val CONTENT_WIDTH = 515f // PAGE_WIDTH - 2 * MARGIN
        private const val BODY_HEIGHT = PAGE_HEIGHT - 2 * MARGIN - FOOTER_HEIGHT
    }
}
