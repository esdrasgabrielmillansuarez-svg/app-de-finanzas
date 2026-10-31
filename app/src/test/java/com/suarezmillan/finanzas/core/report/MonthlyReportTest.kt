package com.suarezmillan.finanzas.core.report

import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas del reporte mensual: qué entra en los totales, cómo se convierte cada
 * movimiento y qué se avisa cuando falta una tasa.
 */
class MonthlyReportTest {

    private val zone: ZoneId = ZoneId.of("America/Caracas")
    private val month = YearMonth.of(2026, 10)

    private fun at(day: Int, hour: Int = 12): Long =
        LocalDate.of(2026, 10, day).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun txn(
        id: String = "t1",
        type: TxnType = TxnType.EXPENSE,
        amountMinor: Long,
        currency: String = "VES",
        occurredAt: Long,
        categoryId: String? = null,
        merchant: String? = null,
        rate: Double? = null,
        base: String? = null,
    ) = TxnEntity(
        id = id,
        type = type,
        amountMinor = amountMinor,
        currency = currency,
        accountId = "acc",
        categoryId = categoryId,
        merchant = merchant,
        occurredAt = occurredAt,
        fxRateToBase = rate,
        fxBaseCurrency = base,
    )

    private val names = mapOf("c1" to "Comida", "c2" to "Sueldo")

    private fun build(
        txns: List<TxnEntity>,
        envelopes: List<ReportEnvelope> = emptyList(),
        rate: (String, String) -> Double? = { _, _ -> null },
    ) = buildMonthlyReport(
        month = month,
        baseCurrency = "VES",
        txns = txns,
        categoryName = { id -> id?.let { names[it] } ?: "" },
        envelopes = envelopes,
        currentRate = rate,
        zone = zone,
        generatedAt = at(30),
    )

    @Test
    fun `solo entran los movimientos del mes`() {
        val report = build(
            listOf(
                txn(id = "in", type = TxnType.EXPENSE, amountMinor = -10_000, occurredAt = at(2)),
                txn(
                    id = "out",
                    type = TxnType.EXPENSE,
                    amountMinor = -99_000,
                    occurredAt = LocalDate.of(2026, 9, 30).atTime(12, 0).atZone(zone).toInstant()
                        .toEpochMilli(),
                ),
            ),
        )

        assertEquals(1, report.rows.size)
        assertEquals(at(2), report.rows.single().occurredAt)
        assertEquals(10_000L, report.summary.expenseMinor)
    }

    @Test
    fun `ingresos y gastos se suman en la divisa base`() {
        val report = build(
            listOf(
                txn(id = "s", type = TxnType.INCOME, amountMinor = 500_000, occurredAt = at(1), categoryId = "c2"),
                txn(id = "c", type = TxnType.EXPENSE, amountMinor = -120_000, occurredAt = at(3), categoryId = "c1"),
                txn(id = "d", type = TxnType.EXPENSE, amountMinor = -80_000, occurredAt = at(4), categoryId = "c1"),
            ),
        )

        assertEquals(500_000L, report.summary.incomeMinor)
        assertEquals(200_000L, report.summary.expenseMinor)
        assertEquals(300_000L, report.summary.balanceMinor)
        assertEquals(3, report.summary.movementCount)
        assertEquals(0, report.summary.missingRateMovements)
    }

    @Test
    fun `los traspasos aparecen pero no cuentan como ingreso ni gasto`() {
        val report = build(
            listOf(
                txn(id = "t", type = TxnType.TRANSFER, amountMinor = -250_000, occurredAt = at(5)),
            ),
        )

        assertEquals(1, report.rows.size)
        assertEquals("Traspaso", report.rows.single().typeLabel)
        assertEquals(0L, report.summary.incomeMinor)
        assertEquals(0L, report.summary.expenseMinor)
    }

    @Test
    fun `un gasto en otra divisa usa su tasa historica`() {
        val report = build(
            listOf(
                txn(
                    id = "usd",
                    amountMinor = -1_000,
                    currency = "USD",
                    occurredAt = at(6),
                    rate = 800.0,
                    base = "VES",
                ),
            ),
            // La tasa actual es otra: no debe pisar la histórica.
            rate = { _, _ -> 999.0 },
        )

        assertEquals(800_000L, report.rows.single().amountInBaseMinor)
        assertEquals(800_000L, report.summary.expenseMinor)
    }

    @Test
    fun `sin tasa no se inventa la equivalencia y se avisa`() {
        val report = build(
            listOf(
                txn(id = "eur", amountMinor = -5_000, currency = "EUR", occurredAt = at(7)),
            ),
        )

        assertNull(report.rows.single().amountInBaseMinor)
        assertEquals(0L, report.summary.expenseMinor)
        assertEquals(1, report.summary.missingRateMovements)
    }

    @Test
    fun `la tasa actual sirve de respaldo si falta la historica`() {
        val report = build(
            listOf(
                txn(id = "old", amountMinor = -2_000, currency = "USD", occurredAt = at(8)),
            ),
            rate = { from, to -> if (from == "USD" && to == "VES") 750.0 else null },
        )

        assertEquals(1_500_000L, report.rows.single().amountInBaseMinor)
        assertEquals(1_500_000L, report.summary.expenseMinor)
    }

    @Test
    fun `los movimientos se ordenan por fecha y llevan su categoria`() {
        val report = build(
            listOf(
                txn(id = "b", amountMinor = -20_000, occurredAt = at(20), categoryId = "c1", merchant = "Panadería"),
                txn(id = "a", amountMinor = -10_000, occurredAt = at(10), categoryId = "c1"),
            ),
        )

        assertEquals(listOf(at(10), at(20)), report.rows.map { it.occurredAt })
        assertEquals("Comida", report.rows.first().categoryName)
        assertEquals("Panadería", report.rows.last().merchant)
    }

    @Test
    fun `los sobres llegan tal cual y saben si estan excedidos`() {
        val report = build(
            txns = emptyList(),
            envelopes = listOf(
                ReportEnvelope("Comida", "🍔", "VES", 100_000, 120_000, -20_000, "Octubre"),
                ReportEnvelope("Transporte", "🚕", "VES", 50_000, 10_000, 40_000, "Octubre"),
            ),
        )

        assertEquals(2, report.envelopes.size)
        assertTrue(report.envelopes.first().isOver)
        assertTrue(!report.envelopes.last().isOver)
    }

    @Test
    fun `sin movimientos el reporte queda en cero`() {
        val report = build(emptyList())

        assertTrue(report.rows.isEmpty())
        assertEquals(0L, report.summary.incomeMinor)
        assertEquals(0L, report.summary.balanceMinor)
        assertEquals(0, report.summary.movementCount)
    }
}
