package com.suarezmillan.finanzas.core.receipt

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas de la bandeja de facturas: qué se rellena solo, qué hay que revisar y
 * qué se guarda al confirmar. Sin Android: se prueba en la JVM.
 */
class ReceiptBatchTest {

    private val zone: ZoneId = ZoneId.of("America/Caracas")
    private val now = LocalDate.of(2026, 10, 30).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun analysis(
        totalMinor: Long? = 150_000L,
        merchant: String? = "Mercado Central",
        currency: String? = null,
        date: LocalDate? = null,
        hint: CategoryHint? = CategoryHint.SUPER,
        confidence: Float = 0.9f,
        items: List<ReceiptItem> = emptyList(),
        details: List<ReceiptDetail> = emptyList(),
        subtotalMinor: Long? = null,
        taxMinor: Long? = null,
        discountMinor: Long? = null,
        pagoMovil: PagoMovilData? = null,
    ) = ReceiptAnalysis(
        merchant = merchant,
        totalMinor = totalMinor,
        currency = currency,
        date = date,
        taxId = null,
        itemsDetected = items.size,
        confidence = confidence,
        categoryHint = hint,
        warnings = emptyList(),
        rawText = "",
        details = details,
        items = items,
        subtotalMinor = subtotalMinor,
        taxMinor = taxMinor,
        discountMinor = discountMinor,
        pagoMovil = pagoMovil,
    )

    private fun draft() = newReceiptDraft(
        id = "d1",
        source = ReceiptSource.CAMERA,
        imageUri = "file:///cache/receipts/factura-1.jpg",
        baseCurrency = "VES",
        now = now,
    )

    private val categories = listOf(
        ReceiptCategory("c1", "Comida"),
        ReceiptCategory("c2", "Supermercado"),
    )

    @Test
    fun `un borrador nuevo espera al OCR y usa la divisa base`() {
        val draft = draft()

        assertEquals(ReceiptStatus.SCANNING, draft.status)
        assertEquals("VES", draft.currency)
        assertEquals(now, draft.occurredAt)
        assertFalse(draft.hasRequiredData)
        assertFalse(draft.needsReview)
    }

    @Test
    fun `la divisa detectada solo se acepta si es una de las dos de la app`() {
        assertEquals("USD", receiptCurrency("usd", baseCurrency = "VES", secondaryCurrency = "USD"))
        assertEquals("VES", receiptCurrency("VES", baseCurrency = "VES", secondaryCurrency = "USD"))
        assertEquals("VES", receiptCurrency("EUR", baseCurrency = "VES", secondaryCurrency = "USD"))
        assertEquals("VES", receiptCurrency(null, baseCurrency = "VES", secondaryCurrency = "USD"))
    }

    @Test
    fun `la fecha de la factura se usa si no esta en el futuro`() {
        val date = LocalDate.of(2026, 10, 12)
        val expected = date.atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(expected, receiptOccurredAt(date, zone, now))
        assertEquals(now, receiptOccurredAt(null, zone, now))
    }

    @Test
    fun `una fecha futura cae en la fecha de carga`() {
        val future = LocalDate.of(2027, 3, 1)

        assertEquals(now, receiptOccurredAt(future, zone, now))
    }

    @Test
    fun `la categoria sugerida respeta el orden del usuario`() {
        assertEquals("c1", suggestCategory(CategoryHint.FOOD, categories))
        assertEquals("c2", suggestCategory(CategoryHint.SUPER, categories))
    }

    @Test
    fun `sin coincidencia no se inventa categoria`() {
        assertNull(suggestCategory(CategoryHint.HEALTH, categories))
        assertNull(suggestCategory(null, categories))
    }

    @Test
    fun `analizar completa el borrador sin guardar nada`() {
        val analyzed = draft().withAnalysis(
            analysis = analysis(merchant = "  Panadería  ", currency = "USD", date = LocalDate.of(2026, 10, 1)),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = listOf(ReceiptCategory("c2", "Mercado")),
            now = now,
            zone = zone,
        )

        assertEquals(ReceiptStatus.READY, analyzed.status)
        assertEquals(150_000L, analyzed.amountMinor)
        assertEquals("USD", analyzed.currency)
        assertEquals("Panadería", analyzed.merchant)
        assertEquals("c2", analyzed.categoryId)
        assertTrue(analyzed.include)
        assertTrue(analyzed.hasRequiredData)
    }

    @Test
    fun `una factura sin total queda para revisar y no se guarda`() {
        val analyzed = draft().withAnalysis(
            analysis = analysis(totalMinor = null),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        assertTrue(analyzed.needsReview)
        assertFalse(analyzed.hasRequiredData)
        assertTrue(draftsToSave(listOf(analyzed)).isEmpty())
    }

    @Test
    fun `sin categoria no se guarda aunque tenga importe`() {
        val analyzed = draft().withAnalysis(
            analysis = analysis(hint = CategoryHint.HEALTH),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        assertNull(analyzed.categoryId)
        assertTrue(analyzed.needsReview)
        assertFalse(analyzed.hasRequiredData)
    }

    @Test
    fun `solo se guardan las incluidas y completas`() {
        val ready = draft().withAnalysis(
            analysis = analysis(),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )
        val excluded = ready.copy(id = "d2", include = false)
        val scanning = draft().copy(id = "d3")

        val toSave = draftsToSave(listOf(ready, excluded, scanning))

        assertEquals(listOf("d1"), toSave.map { it.id })
        assertEquals(1, draftsScanning(listOf(ready, excluded, scanning)))
    }

    @Test
    fun `solo las fotos de camara tienen archivo temporal propio`() {
        assertTrue(draft().ownsTemporaryFile)
        assertFalse(draft().copy(source = ReceiptSource.GALLERY).ownsTemporaryFile)
    }

    @Test
    fun `el desglose empieza con el total leido y suma sus lineas`() {
        val ready = draft().withAnalysis(
            analysis = analysis(totalMinor = 150_000L),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        val breakdown = ready.withBreakdown()
        assertEquals(1, breakdown.parts.size)
        assertEquals(150_000L, breakdown.totalMinor)

        val first = breakdown.parts.first().id
        val edited = breakdown.withPartAmount(first, 120_000L)
        val withSecond = edited.withPartAdded()
        val total = withSecond.withPartAmount(withSecond.parts.last().id, 30_000L)

        assertEquals(2, total.parts.size)
        assertEquals(150_000L, total.totalMinor)
        assertTrue(total.hasRequiredData)
    }

    @Test
    fun `agregar montos es acumulativo y recalcula el total`() {
        val ready = draft().withAnalysis(
            analysis = analysis(totalMinor = null),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        ).withBreakdown()
        val first = ready.parts.first().id
        val secondDraft = ready.withPartAmount(first, 45_000L).withPartAdded()
        val second = secondDraft.parts.last().id
        val both = secondDraft.withPartAmount(second, 55_000L)

        assertEquals(100_000L, both.totalMinor)
    }

    @Test
    fun `un monto negativo cuenta como cero`() {
        val ready = draft().withBreakdown()
        val id = ready.parts.first().id

        assertEquals(0L, ready.withPartAmount(id, -500L).parts.first().amountMinor)
        assertEquals(0L, ready.withPartAmount(id, null).parts.first().amountMinor)
    }

    @Test
    fun `un desglose en cero no habilita el guardado`() {
        val analyzed = draft().withAnalysis(
            analysis = analysis(totalMinor = 150_000L),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )
        val empty = analyzed.withBreakdown().let { it.withPartAmount(it.parts.first().id, 0L) }

        assertNull(empty.totalMinor)
        assertFalse(empty.hasRequiredData)
        assertTrue(draftsToSave(listOf(empty)).isEmpty())
    }

    @Test
    fun `quitar todas las lineas vuelve al importe unico`() {
        val ready = draft().withAnalysis(
            analysis = analysis(totalMinor = 150_000L),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )
        val breakdown = ready.withBreakdown()
        val cleared = breakdown.withoutPart(breakdown.parts.first().id)

        assertTrue(cleared.parts.isEmpty())
        assertEquals(150_000L, cleared.totalMinor)
    }

    @Test
    fun `el desglose se precarga con todas las lineas leidas`() {
        val analyzed = draft().withAnalysis(
            analysis = analysis(
                totalMinor = 210_000L,
                items = listOf(
                    ReceiptItem("LECHE ENTERA 1L", 125_000L),
                    ReceiptItem("PAN DE MOLDE 500G", 85_000L),
                ),
            ),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        assertEquals(2, analyzed.parts.size)
        assertEquals(listOf("LECHE ENTERA 1L", "PAN DE MOLDE 500G"), analyzed.parts.map { it.label })
        assertEquals(210_000L, analyzed.totalMinor)
        assertTrue(analyzed.hasRequiredData)
        assertTrue(analyzed.warnings.isEmpty())
    }

    @Test
    fun `impuestos y descuento completan el total leido`() {
        val analyzed = draft().withAnalysis(
            analysis = analysis(
                totalMinor = 121_000L,
                items = listOf(ReceiptItem("PRODUCTO", 100_000L)),
                taxMinor = 21_000L,
            ),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        assertEquals(2, analyzed.parts.size)
        assertEquals("Impuestos", analyzed.parts.last().label)
        assertEquals(121_000L, analyzed.totalMinor)
    }

    @Test
    fun `un desglose que no cuadra deja el importe unico y avisa`() {
        val analyzed = draft().withAnalysis(
            analysis = analysis(
                totalMinor = 210_000L,
                items = listOf(ReceiptItem("PRODUCTO", 100_000L)),
            ),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        assertTrue(analyzed.parts.isEmpty())
        assertEquals(210_000L, analyzed.totalMinor)
        assertTrue(analyzed.warnings.any { it.contains("no cuadra") })
    }

    @Test
    fun `el concepto de una linea se puede corregir`() {
        val ready = draft().withAnalysis(
            analysis = analysis(
                totalMinor = 150_000L,
                items = listOf(ReceiptItem("LECHE", 150_000L)),
            ),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        val edited = ready.withPartLabel(ready.parts.first().id, "Leche entera")

        assertEquals("Leche entera", edited.parts.first().label)
        assertEquals(150_000L, edited.totalMinor)
    }

    @Test
    fun `la nota guarda todos los campos y el desglose`() {
        val ready = draft().withAnalysis(
            analysis = analysis(
                totalMinor = 150_000L,
                items = listOf(ReceiptItem("LECHE", 150_000L)),
                details = listOf(
                    ReceiptDetail("Operación", "007930190137"),
                    ReceiptDetail("Banco", "BANCO MERCANTIL"),
                ),
            ),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        val note = ready.detailsNote().orEmpty()

        assertTrue(note.contains("Operación: 007930190137"))
        assertTrue(note.contains("Banco: BANCO MERCANTIL"))
        assertTrue(note.contains("Desglose: LECHE"))
    }

    @Test
    fun `un pago movil se carga completo y siempre en bolivares`() {
        val pago = PagoMovilData(
            amountMinor = 500_000L,
            date = LocalDate.of(2026, 11, 6),
            operation = "007930190137",
            idNumber = "17626843",
            origin = "0102****8936",
            destination = "04145687412",
            bankCode = "0105",
            bankName = "BANCO MERCANTIL",
        )
        val analyzed = draft().withAnalysis(
            analysis = analysis(
                totalMinor = 500_000L,
                merchant = "PagoMóvil 04145687412",
                currency = "USD", // el comprobante manda: un PagoMóvil es VES
                hint = null,
                pagoMovil = pago,
            ),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        assertEquals("VES", analyzed.currency)
        assertEquals(500_000L, analyzed.totalMinor)
        assertEquals("PagoMóvil 04145687412", analyzed.merchant)
        assertTrue(analyzed.isPagoMovil)
        assertTrue(analyzed.parts.isEmpty())
        assertEquals(7, analyzed.details.size)
        assertNull(analyzed.categoryId)
        assertTrue(analyzed.needsReview)
    }

    @Test
    fun `la nota de un pago movil guarda todos los campos`() {
        val pago = PagoMovilData(
            amountMinor = 500_000L,
            date = LocalDate.of(2026, 11, 6),
            operation = "007930190137",
            idNumber = "17626843",
            origin = "0102****8936",
            destination = "04145687412",
            bankCode = "0105",
            bankName = "BANCO MERCANTIL",
        )
        val analyzed = draft().withAnalysis(
            analysis = analysis(
                totalMinor = 500_000L,
                merchant = "PagoMóvil 04145687412",
                hint = null,
                pagoMovil = pago,
            ),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        val note = analyzed.detailsNote().orEmpty()

        assertTrue(note.startsWith("PagoMóvil"))
        assertTrue(note.contains("Monto: Bs. 5.000,00"))
        assertTrue(note.contains("Fecha: 06/11/2026"))
        assertTrue(note.contains("Operación: 007930190137"))
        assertTrue(note.contains("Identificación: 17626843"))
        assertTrue(note.contains("Origen: 0102****8936"))
        assertTrue(note.contains("Destino: 04145687412"))
        assertTrue(note.contains("Banco: 0105 - BANCO MERCANTIL"))
    }

    @Test
    fun `el desglose respeta el tope de lineas`() {
        var breakdown = draft().withBreakdown()
        repeat(MAX_RECEIPT_PARTS + 5) { breakdown = breakdown.withPartAdded() }

        assertEquals(MAX_RECEIPT_PARTS, breakdown.parts.size)
    }

    @Test
    fun `reanalizar la factura descarta el desglose anterior`() {
        val ready = draft().withAnalysis(
            analysis = analysis(totalMinor = 150_000L),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )
        val reanalyzed = ready.withBreakdown().withAnalysis(
            analysis = analysis(totalMinor = 200_000L),
            baseCurrency = "VES",
            secondaryCurrency = "USD",
            categories = categories,
            now = now,
            zone = zone,
        )

        assertTrue(reanalyzed.parts.isEmpty())
        assertEquals(200_000L, reanalyzed.totalMinor)
    }
}
