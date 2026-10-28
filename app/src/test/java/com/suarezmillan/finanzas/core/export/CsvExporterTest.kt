package com.suarezmillan.finanzas.core.export

import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private val txn = TxnEntity(
        id = "t1",
        type = TxnType.EXPENSE,
        amountMinor = -125_000,
        currency = "ARS",
        accountId = "acc-cash",
        categoryId = "cat-super",
        merchant = "Súper, \"La Esquina\"",
        note = "compra semanal",
    )

    @Test
    fun `cabecera y fila con importe decimal`() {
        val csv = CsvExporter.build(listOf(txn), categoryName = { "Súper" })
        val lines = csv.lines()

        assertEquals("fecha,tipo,importe,divisa,categoria,comercio,nota,tasa_a_base", lines[0])
        assertTrue(lines[1].contains("-1250.00"))
        assertTrue(lines[1].contains("ARS"))
        assertTrue(lines[1].contains("\"Súper, \"\"La Esquina\"\"\""))
    }

    @Test
    fun `sin movimientos solo cabecera`() {
        val csv = CsvExporter.build(emptyList(), categoryName = { "" })

        assertEquals(1, csv.lines().size)
        assertTrue(csv.startsWith("fecha,tipo"))
    }

    @Test
    fun `escapado rfc 4180`() {
        assertEquals("simple", CsvExporter.escape("simple"))
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
        assertEquals("\"a\"\"b\"", CsvExporter.escape("a\"b"))
        assertEquals("\"con\nsalto\"", CsvExporter.escape("con\nsalto"))
    }
}
