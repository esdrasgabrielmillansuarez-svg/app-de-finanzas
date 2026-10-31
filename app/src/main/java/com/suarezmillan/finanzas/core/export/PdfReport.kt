package com.suarezmillan.finanzas.core.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.suarezmillan.finanzas.core.branding.Branding
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.core.report.MonthlyReport
import com.suarezmillan.finanzas.core.report.ReportRow
import com.suarezmillan.finanzas.data.local.entity.TxnType
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Reporte mensual en PDF (A4, 595×842 pt).
 *
 * Se dibuja con [PdfDocument]: sin librerías nuevas y sin salir del teléfono.
 * Cada página repite el pie con la marca y el número de página; si la tabla no
 * cabe, continúa en la siguiente.
 */
fun buildMonthlyReportPdf(
    report: MonthlyReport,
    zone: ZoneId = ZoneId.systemDefault(),
): ByteArray = ReportPdf(report, zone).render()

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val CONTENT_BOTTOM = PAGE_HEIGHT - 54f
private const val FOOTER_Y = PAGE_HEIGHT - 28f

private const val INK = 0xFF1B1B1B.toInt()
private const val MUTED = 0xFF5F6368.toInt()
private const val RULE = 0xFFDADCE0.toInt()
private const val FILL = 0xFFF1F3F4.toInt()

private val BRAND = 0xFF0F766E.toInt()
private val DANGER = 0xFFB3261E.toInt()

private fun paint(
    size: Float,
    color: Int = INK,
    bold: Boolean = false,
    align: Paint.Align = Paint.Align.LEFT,
): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = size
    this.color = color
    typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    textAlign = align
}

private class ReportPdf(
    private val report: MonthlyReport,
    private val zone: ZoneId,
) {
    private val document = PdfDocument()
    private var page: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var pageIndex = 0
    private var y = MARGIN

    private val titlePaint = paint(20f, INK, bold = true)
    private val monthPaint = paint(15f, BRAND, bold = true)
    private val subtitlePaint = paint(9f, MUTED)
    private val sectionPaint = paint(10f, BRAND, bold = true)
    private val bodyPaint = paint(9f, INK)
    private val bodyBoldPaint = paint(9f, INK, bold = true)
    private val smallPaint = paint(8f, MUTED)
    private val headerPaint = paint(8f, MUTED, bold = true)
    private val amountPaint = paint(9f, INK, align = Paint.Align.RIGHT)
    private val amountHeaderPaint = paint(8f, MUTED, bold = true, align = Paint.Align.RIGHT)
    private val footerPaint = paint(7.5f, MUTED)
    private val footerRightPaint = paint(7.5f, MUTED, align = Paint.Align.RIGHT)
    private val rulePaint = Paint().apply {
        color = RULE
        strokeWidth = 0.7f
    }
    private val rowDatePaint = paint(8f, MUTED)
    private val smallRightPaint = paint(7.5f, MUTED, align = Paint.Align.RIGHT)

    fun render(): ByteArray {
        try {
            newPage()
            drawCoverHeader()
            drawSummary()
            drawEnvelopes()
            drawMovements()
            page?.let { document.finishPage(it) }
            page = null

            val out = ByteArrayOutputStream()
            document.writeTo(out)
            return out.toByteArray()
        } finally {
            runCatching { document.close() }
        }
    }

    // ------------------------------------------------------------------ páginas

    private fun newPage() {
        page?.let { document.finishPage(it) }
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
        val next = document.startPage(info)
        page = next
        canvas = next.canvas
        y = MARGIN

        if (pageIndex > 0) {
            canvas.drawText(
                "${Branding.APP_NAME} · Reporte de ${monthLabel()}",
                MARGIN,
                y + 8f,
                bodyBoldPaint,
            )
            canvas.drawLine(MARGIN, y + 16f, PAGE_WIDTH - MARGIN, y + 16f, rulePaint)
            y += 30f
        }

        canvas.drawText("${Branding.APP_NAME} · ${Branding.CREATOR}", MARGIN, FOOTER_Y, footerPaint)
        canvas.drawText("Página ${pageIndex + 1}", PAGE_WIDTH - MARGIN, FOOTER_Y, footerRightPaint)
        pageIndex++
    }

    private fun ensureSpace(needed: Float) {
        if (y + needed > CONTENT_BOTTOM) newPage()
    }

    // ------------------------------------------------------------------ cabecera y resumen

    private fun drawCoverHeader() {
        canvas.drawText("Reporte del mes", MARGIN, y + 18f, titlePaint)
        y += 30f
        canvas.drawText(monthLabel(), MARGIN, y, monthPaint)
        y += 14f
        val generated = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .format(Instant.ofEpochMilli(report.generatedAt).atZone(zone))
        canvas.drawText(
            "Generado el $generated · Moneda base ${report.baseCurrency}",
            MARGIN,
            y,
            subtitlePaint,
        )
        y += 12f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
        y += 22f
    }

    private fun drawSummary() {
        canvas.drawText("RESUMEN", MARGIN, y, sectionPaint)
        y += 16f

        val summary = report.summary
        amountRow("Ingresos", "+ ", summary.incomeMinor, BRAND, bold = false)
        amountRow("Gastos", "− ", summary.expenseMinor, INK, bold = false)
        val balance = summary.balanceMinor
        amountRow(
            label = "Balance",
            sign = if (balance < 0) "− " else "+ ",
            amountMinor = abs(balance),
            color = if (balance < 0) DANGER else BRAND,
            bold = true,
        )

        val meta = buildString {
            append(summary.movementCount)
            append(if (summary.movementCount == 1) " movimiento" else " movimientos")
            if (summary.missingRateMovements > 0) {
                append(" · ")
                append(summary.missingRateMovements)
                append(" sin tasa (no sumados)")
            }
        }
        canvas.drawText(meta, MARGIN, y + 6f, smallPaint)
        y += 18f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
        y += 20f
    }

    private fun amountRow(label: String, sign: String, amountMinor: Long, color: Int, bold: Boolean) {
        val labelPaint = if (bold) bodyBoldPaint else bodyPaint
        val valuePaint = paint(if (bold) 10f else 9f, color, bold, Paint.Align.RIGHT)
        canvas.drawText(label, MARGIN, y + 9f, labelPaint)
        canvas.drawText(
            sign + MoneyFormat.format(amountMinor, report.baseCurrency),
            PAGE_WIDTH - MARGIN,
            y + 9f,
            valuePaint,
        )
        y += 16f
    }

    // ------------------------------------------------------------------ sobres

    private fun drawEnvelopes() {
        if (report.envelopes.isEmpty()) return
        ensureSpace(50f)
        canvas.drawText("SOBRES · PERIODO VIGENTE", MARGIN, y, sectionPaint)
        y += 16f

        report.envelopes.forEach { envelope ->
            ensureSpace(34f)
            val title = "${envelope.name} · ${envelope.windowLabel}"
            canvas.drawText(ellipsize(title, 300f, bodyPaint), MARGIN, y + 9f, bodyPaint)
            canvas.drawText(
                "${MoneyFormat.format(envelope.spentMinor, envelope.currency)} / " +
                    MoneyFormat.format(envelope.limitMinor, envelope.currency),
                PAGE_WIDTH - MARGIN,
                y + 9f,
                amountPaint,
            )
            y += 14f

            val (status, statusPaint) = if (envelope.isOver) {
                "Excedido en ${MoneyFormat.format(abs(envelope.remainingMinor), envelope.currency)}" to
                    paint(8f, DANGER)
            } else {
                "Queda ${MoneyFormat.format(envelope.remainingMinor, envelope.currency)}" to smallPaint
            }
            canvas.drawText(status, MARGIN + 6f, y + 6f, statusPaint)
            y += 12f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
            y += 8f
        }
        y += 8f
    }

    // ------------------------------------------------------------------ movimientos

    private fun drawMovements() {
        ensureSpace(52f)
        canvas.drawText("MOVIMIENTOS", MARGIN, y, sectionPaint)
        y += 14f

        if (report.rows.isEmpty()) {
            canvas.drawText("Sin movimientos registrados este mes.", MARGIN, y + 8f, bodyPaint)
            y += 18f
            return
        }

        drawTableHeader()

        report.rows.forEach { row ->
            val twoLines = row.amountInBaseMinor != null && row.currency != report.baseCurrency
            ensureSpace(if (twoLines) 30f else 22f)
            drawRow(row, twoLines)
        }
    }

    private fun drawTableHeader() {
        val top = y
        canvas.drawRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + 16f, Paint().apply { color = FILL })
        val baseline = top + 11f
        canvas.drawText("Fecha", COL_DATE, baseline, headerPaint)
        canvas.drawText("Tipo", COL_TYPE, baseline, headerPaint)
        canvas.drawText("Categoría", COL_CATEGORY, baseline, headerPaint)
        canvas.drawText("Comercio", COL_MERCHANT, baseline, headerPaint)
        canvas.drawText("Importe", RIGHT_EDGE, baseline, amountHeaderPaint)
        y = top + 20f
    }

    private fun drawRow(row: ReportRow, twoLines: Boolean) {
        val baseline = y + 9f
        val date = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            .format(Instant.ofEpochMilli(row.occurredAt).atZone(zone))
        canvas.drawText(date, COL_DATE, baseline, rowDatePaint)
        canvas.drawText(row.typeLabel, COL_TYPE, baseline, rowDatePaint)
        canvas.drawText(
            ellipsize(row.categoryName.ifBlank { "Sin categoría" }, COL_MERCHANT - COL_CATEGORY - 8f, bodyPaint),
            COL_CATEGORY,
            baseline,
            bodyPaint,
        )
        val merchant = row.merchant.orEmpty()
        if (merchant.isNotEmpty()) {
            canvas.drawText(
                ellipsize(merchant, RIGHT_EDGE - 86f - COL_MERCHANT, bodyPaint),
                COL_MERCHANT,
                baseline,
                bodyPaint,
            )
        }

        val amountColor = when (row.type) {
            TxnType.INCOME -> BRAND
            TxnType.TRANSFER -> MUTED
            TxnType.EXPENSE -> INK
        }
        canvas.drawText(
            MoneyFormat.formatSigned(row.amountMinor, row.currency),
            RIGHT_EDGE,
            baseline,
            paint(9f, amountColor, align = Paint.Align.RIGHT),
        )
        y += 14f

        if (twoLines) {
            canvas.drawText(
                "≈ ${MoneyFormat.format(row.amountInBaseMinor ?: 0L, report.baseCurrency)}",
                RIGHT_EDGE,
                y + 7f,
                smallRightPaint,
            )
            y += 10f
        }

        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, rulePaint)
        y += 7f
    }

    // ------------------------------------------------------------------ utilidades

    private fun monthLabel(): String {
        val locale = Locale("es", "AR")
        val raw = report.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    private fun ellipsize(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val available = maxWidth - paint.measureText(ellipsis)
        if (available <= 0f) return ellipsis
        val count = paint.breakText(text, true, available, null)
        return text.substring(0, count).trimEnd() + ellipsis
    }

    private companion object {
        const val COL_DATE = MARGIN
        const val COL_TYPE = 94f
        const val COL_CATEGORY = 150f
        const val COL_MERCHANT = 264f
        const val RIGHT_EDGE = PAGE_WIDTH - MARGIN
    }
}
