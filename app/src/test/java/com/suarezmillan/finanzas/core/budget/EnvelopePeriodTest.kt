package com.suarezmillan.finanzas.core.budget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EnvelopePeriodTest {

    @Test
    fun `mensual va del primero al ultimo dia del mes`() {
        val window = EnvelopePeriod.MONTHLY.windowFor(LocalDate.of(2026, 2, 10))

        assertEquals(LocalDate.of(2026, 2, 1), window.start)
        assertEquals(LocalDate.of(2026, 3, 1), window.endExclusive)
        assertEquals(LocalDate.of(2026, 2, 28), window.lastDay)
    }

    @Test
    fun `quincenal corta el dia 15 y el 16`() {
        val firstHalf = EnvelopePeriod.BIWEEKLY.windowFor(LocalDate.of(2026, 10, 15))
        val secondHalf = EnvelopePeriod.BIWEEKLY.windowFor(LocalDate.of(2026, 10, 16))

        assertEquals(LocalDate.of(2026, 10, 1), firstHalf.start)
        assertEquals(LocalDate.of(2026, 10, 16), firstHalf.endExclusive)
        assertEquals(LocalDate.of(2026, 10, 16), secondHalf.start)
        assertEquals(LocalDate.of(2026, 11, 1), secondHalf.endExclusive)
    }

    @Test
    fun `quincenal de febrero termina el 28 o 29`() {
        val leap = EnvelopePeriod.BIWEEKLY.windowFor(LocalDate.of(2024, 2, 20))
        val common = EnvelopePeriod.BIWEEKLY.windowFor(LocalDate.of(2026, 2, 20))

        assertEquals(LocalDate.of(2024, 2, 29), leap.lastDay)
        assertEquals(LocalDate.of(2026, 2, 28), common.lastDay)
        assertEquals(LocalDate.of(2024, 3, 1), leap.endExclusive)
    }

    @Test
    fun `semanal va de lunes a domingo`() {
        // 29 oct 2026 es jueves.
        val window = EnvelopePeriod.WEEKLY.windowFor(LocalDate.of(2026, 10, 29))

        assertEquals(LocalDate.of(2026, 10, 26), window.start)
        assertEquals(LocalDate.of(2026, 11, 1), window.lastDay)
        assertEquals(LocalDate.of(2026, 11, 2), window.endExclusive)
    }

    @Test
    fun `el periodo anterior termina donde empieza el actual`() {
        val today = LocalDate.of(2026, 10, 1)
        val previous = EnvelopePeriod.MONTHLY.previousWindow(today)

        assertEquals(LocalDate.of(2026, 9, 1), previous.start)
        assertEquals(today, previous.endExclusive)

        val weeklyPrevious = EnvelopePeriod.WEEKLY.previousWindow(LocalDate.of(2026, 11, 2))
        assertEquals(LocalDate.of(2026, 10, 26), weeklyPrevious.start)
        assertEquals(LocalDate.of(2026, 11, 2), weeklyPrevious.endExclusive)
    }

    @Test
    fun `etiqueta el rango sin repetir el mes`() {
        val sameMonth = EnvelopePeriod.BIWEEKLY.windowFor(LocalDate.of(2026, 4, 3))
        val acrossMonths = EnvelopePeriod.WEEKLY.windowFor(LocalDate.of(2026, 10, 29))

        assertEquals("1–15 abr", sameMonth.label())
        assertEquals("26 oct – 1 nov", acrossMonths.label())
    }
}
