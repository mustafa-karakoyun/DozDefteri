package app.dozdefteri.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import app.dozdefteri.domain.DayStatus
import java.io.OutputStream
import javax.inject.Inject

/**
 * Renders a [PdfReport] as an A4 doctor summary (595 × 842 pt, 40 pt margins) with [PdfDocument]. It fits on one
 * page for typical medication lists; longer lists continue on further pages. Every page carries the disclaimer
 * and an "n/total" page number.
 */
class PdfExporter @Inject constructor() {

    private val titlePaint = textPaint(TITLE_SIZE, bold = true)
    private val bodyPaint = textPaint(BODY_SIZE, bold = false)
    private val boldPaint = textPaint(BODY_SIZE, bold = true)
    private val smallPaint = textPaint(SMALL_SIZE, bold = false)
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun write(report: PdfReport, output: OutputStream) {
        val pages = paginate(buildItems(report))
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, items ->
                val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(info)
                var y = MARGIN.toFloat()
                items.forEach { item ->
                    draw(page.canvas, item, y)
                    y += item.height
                }
                drawFooter(page.canvas, report, index + 1, pages.size)
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
        data class Strip(val days: List<DayStatus>) : Item {
            override val height: Float get() = STRIP_HEIGHT + 8f
        }
    }

    private fun buildItems(report: PdfReport): List<Item> = buildList {
        add(Item.Text(report.title, titlePaint, TITLE_SIZE + 10f))
        report.subtitleLines.forEach { addAll(wrap(it, bodyPaint)) }
        add(Item.Spacer(GAP))
        add(Item.Text(report.adherenceLine, boldPaint, BODY_SIZE + 8f))
        add(Item.Spacer(GAP / 2))
        if (report.medicationLines.isEmpty()) {
            addAll(wrap(report.emptyText, bodyPaint))
        } else {
            report.medicationLines.forEach { addAll(wrap(it, bodyPaint)) }
        }
        if (report.days.isNotEmpty()) {
            add(Item.Spacer(GAP))
            add(Item.Strip(report.days))
            addAll(wrap(report.stripLegend, smallPaint))
        }
        add(Item.Spacer(GAP))
        addAll(wrap(report.footnote, smallPaint))
    }

    private fun paginate(items: List<Item>): List<List<Item>> {
        val pages = mutableListOf<MutableList<Item>>(mutableListOf())
        var used = 0f
        items.forEach { item ->
            if (used + item.height > BODY_HEIGHT && pages.last().isNotEmpty()) {
                pages += mutableListOf<Item>()
                used = 0f
                if (item is Item.Spacer) return@forEach
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
            is Item.Strip -> {
                val gap = 1.5f
                val width = (CONTENT_WIDTH - gap * (item.days.size - 1)) / item.days.size
                item.days.forEachIndexed { index, status ->
                    barPaint.color = stripColor(status)
                    val left = MARGIN + index * (width + gap)
                    val barHeight = if (status == DayStatus.NONE) STRIP_HEIGHT / 3 else STRIP_HEIGHT
                    canvas.drawRect(left, top + STRIP_HEIGHT - barHeight, left + width, top + STRIP_HEIGHT, barPaint)
                }
            }
        }
    }

    private fun stripColor(status: DayStatus): Int = when (status) {
        DayStatus.ALL_TAKEN -> COLOR_TAKEN
        DayStatus.PARTIAL -> COLOR_PARTIAL
        DayStatus.MISSED -> COLOR_MISSED
        DayStatus.NONE -> COLOR_NONE
    }

    private fun drawFooter(canvas: Canvas, report: PdfReport, page: Int, total: Int) {
        val lines = wrapLines(report.disclaimer, smallPaint, CONTENT_WIDTH * 0.8f)
        var y = PAGE_HEIGHT - MARGIN - (lines.size - 1) * (SMALL_SIZE + 2f)
        lines.forEach { line ->
            canvas.drawText(line, MARGIN.toFloat(), y, smallPaint)
            y += SMALL_SIZE + 2f
        }
        val pageText = String.format(report.pageFormat, page, total)
        canvas.drawText(
            pageText,
            MARGIN + CONTENT_WIDTH - smallPaint.measureText(pageText),
            (PAGE_HEIGHT - MARGIN).toFloat(),
            smallPaint
        )
    }

    private fun wrap(text: String, paint: TextPaint): List<Item> =
        wrapLines(text, paint, CONTENT_WIDTH).map { Item.Text(it, paint, paint.textSize + 5f) }

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
        color = 0xFF1C1A17.toInt()
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40
        private const val TITLE_SIZE = 16f
        private const val BODY_SIZE = 11f
        private const val SMALL_SIZE = 8f
        private const val GAP = 14f
        private const val STRIP_HEIGHT = 28f
        private const val FOOTER_HEIGHT = 40f
        private const val CONTENT_WIDTH = 515f // PAGE_WIDTH - 2 * MARGIN
        private const val BODY_HEIGHT = PAGE_HEIGHT - 2 * MARGIN - FOOTER_HEIGHT

        // Report colors mirror the design tokens (teal/600, amber/700, clay/700, sand/200).
        private val COLOR_TAKEN = 0xFF17685D.toInt()
        private val COLOR_PARTIAL = 0xFF7A5000.toInt()
        private val COLOR_MISSED = 0xFF87472A.toInt()
        private val COLOR_NONE = 0xFFDCD3C5.toInt()
    }
}
