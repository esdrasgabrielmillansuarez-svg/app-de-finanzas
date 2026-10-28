package com.suarezmillan.finanzas.core.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachEngineTest {

    private val base = CoachContext(
        movementCount = 12,
        spentMinor = 50_000L,
        budgetLimitMinor = 100_000L,
        daysLeftInMonth = 10,
    )

    @Test
    fun `un pago de hoy gana sobre sobres y presupuesto`() {
        val message = CoachEngine.message(
            base.copy(
                overBudgetEnvelopes = listOf("Súper"),
                duePayments = listOf(DuePayment("Alquiler", 0, "ARS")),
            ),
        )

        assertTrue(message.title.contains("Alquiler"))
        assertEquals(CoachTone.WARN, message.tone)
        assertEquals(CoachAction.OPEN_PLAN, message.action)
    }

    @Test
    fun `un pago vencido se anuncia como vencido, no como hoy`() {
        val message = CoachEngine.message(
            base.copy(duePayments = listOf(DuePayment("Internet", -2, "ARS"))),
        )

        assertTrue(message.title.contains("venció"))
        assertTrue(message.body.contains("2 días"))
    }

    @Test
    fun `un pago de manana usa lenguaje humano`() {
        val message = CoachEngine.message(
            base.copy(duePayments = listOf(DuePayment("Luz", 1, "ARS"))),
        )

        assertTrue(message.title.contains("mañana"))
        assertEquals(CoachTone.NUDGE, message.tone)
    }

    @Test
    fun `el pago mas cercano tiene prioridad`() {
        val message = CoachEngine.message(
            base.copy(
                duePayments = listOf(
                    DuePayment("Lejano", 3, "ARS"),
                    DuePayment("Cercano", 1, "ARS"),
                ),
            ),
        )

        assertTrue(message.title.contains("Cercano"))
    }

    @Test
    fun `un sobre pasado se avisa sin culpa`() {
        val message = CoachEngine.message(base.copy(overBudgetEnvelopes = listOf("Salidas")))

        assertTrue(message.title.contains("Salidas"))
        assertEquals(CoachTone.WARN, message.tone)
        assertTrue(message.body.contains("No es el fin del mundo"))
    }

    @Test
    fun `un sobre al limite se avisa antes de que se pase`() {
        val message = CoachEngine.message(base.copy(nearLimitEnvelopes = listOf("Súper")))

        assertTrue(message.title.contains("límite"))
        assertEquals(CoachTone.NUDGE, message.tone)
    }

    @Test
    fun `sin movimientos invita a anotar el primero`() {
        val message = CoachEngine.message(base.copy(movementCount = 0))

        assertEquals(CoachAction.OPEN_QUICK, message.action)
        assertEquals("🌱", message.emoji)
    }

    @Test
    fun `presupuesto excedido tiene su propio mensaje`() {
        val message = CoachEngine.message(
            base.copy(spentMinor = 150_000L, budgetLimitMinor = 100_000L),
        )

        assertTrue(message.title.contains("presupuesto"))
        assertEquals(CoachTone.WARN, message.tone)
    }

    @Test
    fun `mes ganador cerca del cierre celebra`() {
        val message = CoachEngine.message(
            base.copy(spentMinor = 50_000L, budgetLimitMinor = 100_000L, daysLeftInMonth = 5),
        )

        assertEquals(CoachTone.CELEBRATE, message.tone)
    }

    @Test
    fun `sprint final ajustado avisa`() {
        val message = CoachEngine.message(
            base.copy(spentMinor = 90_000L, budgetLimitMinor = 100_000L, daysLeftInMonth = 4),
        )

        assertTrue(message.title.contains("sprint"))
        assertEquals(CoachTone.NUDGE, message.tone)
    }

    @Test
    fun `las variantes rotan y no repiten la misma clave`() {
        val first = CoachEngine.message(base.copy(movementCount = 0), variant = 0)
        val second = CoachEngine.message(base.copy(movementCount = 0), variant = 1)
        val third = CoachEngine.message(base.copy(movementCount = 0), variant = 2)

        assertNotEquals(first.key, second.key)
        assertEquals(first.key, third.key) // dos variantes → vuelve a la primera
    }

    @Test
    fun `variantes negativas no rompen`() {
        val message = CoachEngine.message(base, variant = -5)
        assertTrue(message.title.isNotBlank())
    }

    @Test
    fun `hitos solo en los numeros elegidos`() {
        assertNull(CoachEngine.milestone(0))
        assertTrue(CoachEngine.milestone(1)!!.contains("Primer movimiento"))
        assertTrue(CoachEngine.milestone(10)!!.contains("Diez"))
        assertNull(CoachEngine.milestone(7))
        assertNull(CoachEngine.milestone(999))
    }
}
