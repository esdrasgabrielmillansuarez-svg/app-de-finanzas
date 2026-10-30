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
    ) = ReceiptAnalysis(
        merchant = merchant,
        totalMinor = totalMinor,
        currency = currency,
        date = date,
        taxId = null,
        itemsDetected = 3,
        confidence = confidence,
        categoryHint = hint,
        warnings = emptyList(),
        rawText = "",
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
}
