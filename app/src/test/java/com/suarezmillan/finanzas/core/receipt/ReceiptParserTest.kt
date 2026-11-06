package com.suarezmillan.finanzas.core.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {

    private val superTicket = """
        SUPERMERCADO LA ANONIMA
        CUIT: 30-71234567-8
        FECHA: 12/10/2026
        LECHE ENTERA 1L          1.250,00
        PAN DE MOLDE 500G          850,00
        SUBTOTAL                 2.100,00
        TOTAL A PAGAR            2.100,00
        IVA 21%                    364,46
    """.trimIndent()

    private val fuelTicket = """
        ESTACION YPF
        01-10-26 18:45
        NAFTA SUPER 95
        IMPORTE        $ 45.300,00
        TOTAL
        45.300,00
    """.trimIndent()

    private val euroReceipt = """
        RESTAURANTE EL SOL
        C/ Mayor 12
        Fecha 05/09/2026
        TOTAL ......... 48,50 €
    """.trimIndent()

    private val pagoMovil = """
        Comprobante de operación
        PagomóvilBDV Personas
        5.000,00 Bs
        Fecha: 06/11/2026
        Operación: 007930190137
        Identificación: 17626843
        Origen: 0102****8936
        Destino: 04145687412
        Banco: 0105 - BANCO MERCANTIL
    """.trimIndent()

    @Test
    fun `ticket de supermercado`() {
        val result = ReceiptParser.parse(superTicket)

        assertEquals(210_000L, result.totalMinor)
        assertEquals(LocalDate.of(2026, 10, 12), result.date)
        assertEquals("SUPERMERCADO LA ANONIMA", result.merchant)
        assertEquals("30-71234567-8", result.taxId)
        assertEquals(CategoryHint.SUPER, result.categoryHint)
        assertTrue(result.itemsDetected >= 2)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun `total en la linea siguiente`() {
        val result = ReceiptParser.parse(fuelTicket)

        assertEquals(4_530_000L, result.totalMinor)
        assertEquals(LocalDate.of(2026, 10, 1), result.date)
        assertEquals(CategoryHint.TRANSPORT, result.categoryHint)
    }

    @Test
    fun `factura en euros`() {
        val result = ReceiptParser.parse(euroReceipt)

        assertEquals(4_850L, result.totalMinor)
        assertEquals("EUR", result.currency)
        assertEquals(CategoryHint.FOOD, result.categoryHint)
    }

    @Test
    fun `detecta dolares explicitos`() {
        val result = ReceiptParser.parse("TIENDA\nTOTAL US$ 120,00")

        assertEquals(12_000L, result.totalMinor)
        assertEquals("USD", result.currency)
    }

    @Test
    fun `comprobante de pago movil trae todos los campos`() {
        val result = ReceiptParser.parse(pagoMovil, defaultCurrency = "VES")

        assertEquals(500_000L, result.totalMinor)
        assertEquals("VES", result.currency)
        assertEquals(LocalDate.of(2026, 11, 6), result.date)
        assertEquals("BANCO MERCANTIL", result.merchant)

        val fields = result.details.associate { it.label to it.value }
        assertEquals("06/11/2026", fields["Fecha"])
        assertEquals("007930190137", fields["Operación"])
        assertEquals("17626843", fields["Identificación"])
        assertEquals("0102****8936", fields["Origen"])
        assertEquals("04145687412", fields["Destino"])
        assertEquals("0105 - BANCO MERCANTIL", fields["Banco"])
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `el desglose lee cada concepto con su importe`() {
        val result = ReceiptParser.parse(superTicket)

        assertEquals(2, result.items.size)
        assertEquals("LECHE ENTERA 1L", result.items[0].label)
        assertEquals(125_000L, result.items[0].amountMinor)
        assertEquals("PAN DE MOLDE 500G", result.items[1].label)
        assertEquals(85_000L, result.items[1].amountMinor)
        assertEquals(210_000L, result.subtotalMinor)
        assertEquals(36_446L, result.taxMinor)
    }

    @Test
    fun `texto basura no inventa datos`() {
        val result = ReceiptParser.parse("ASD\n***")

        assertNull(result.totalMinor)
        assertNull(result.date)
        assertTrue(result.confidence < 0.6f)
        assertTrue(result.warnings.any { it.contains("total", ignoreCase = true) })
    }

    @Test
    fun `total en cero no se acepta como valido`() {
        val result = ReceiptParser.parse("KIOSCO\nTOTAL 0,00")

        assertEquals(0L, result.totalMinor)
        assertTrue(result.warnings.any { it.contains("inválido", ignoreCase = true) })
    }
}
