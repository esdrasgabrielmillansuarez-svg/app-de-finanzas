package com.suarezmillan.finanzas.core.budget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EnvelopeCarryTest {

    private val zone = ZoneId.of("UTC")

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun carry(
        today: LocalDate,
        created: LocalDate,
        period: EnvelopePeriod = EnvelopePeriod.MONTHLY,
        limit: Long = 100_000L,
        spentByStart: Map<LocalDate, Long> = emptyMap(),
    ): Long = EnvelopeCarry.carriedOver(
        baseLimitMinor = limit,
        createdAtMillis = millis(created),
        period = period,
        today = today,
        zone = zone,
    ) { from, _ ->
        spentByStart[Instant.ofEpochMilli(from).atZone(zone).toLocalDate()] ?: 0L
    }

    @Test
    fun `sin periodos cerrados no arrastra`() {
        val today = LocalDate.of(2026, 2, 10)
        val createdMidPeriod = LocalDate.of(2026, 2, 3)

        assertEquals(0L, carry(today, createdMidPeriod))
    }

    @Test
    fun `el periodo de creacion no arrastra`() {
        val today = LocalDate.of(2026, 2, 10)
        val createdMidJanuary = LocalDate.of(2026, 1, 20)

        // Enero se creó a mitad de periodo: no es comparable y se omite.
        assertEquals(0L, carry(today, createdMidJanuary))
    }

    @Test
    fun `arrastra el sobrante del mes anterior`() {
        val today = LocalDate.of(2026, 2, 10)
        val created = LocalDate.of(2026, 1, 1)

        val result = carry(
            today = today,
            created = created,
            spentByStart = mapOf(LocalDate.of(2026, 1, 1) to 60_000L),
        )

        assertEquals(40_000L, result)
    }

    @Test
    fun `acumula el sobrante de varios periodos`() {
        val today = LocalDate.of(2026, 3, 10)
        val created = LocalDate.of(2026, 1, 1)

        val result = carry(
            today = today,
            created = created,
            spentByStart = mapOf(
                LocalDate.of(2026, 1, 1) to 60_000L,
                LocalDate.of(2026, 2, 1) to 30_000L,
            ),
        )

        // Enero: 100 - 60 = 40. Febrero: 100 + 40 - 30 = 110.
        assertEquals(110_000L, result)
    }

    @Test
    fun `pasarse no genera deuda`() {
        val today = LocalDate.of(2026, 3, 10)
        val created = LocalDate.of(2026, 1, 1)

        val result = carry(
            today = today,
            created = created,
            spentByStart = mapOf(LocalDate.of(2026, 1, 1) to 150_000L),
        )

        // Enero queda en 0, no en -50.000. Febrero sin gasto aporta 100.000.
        assertEquals(100_000L, result)
    }

    @Test
    fun `quincenal usa los cortes reales del calendario`() {
        val today = LocalDate.of(2026, 2, 20)
        val created = LocalDate.of(2026, 1, 1)

        val result = carry(
            today = today,
            created = created,
            period = EnvelopePeriod.BIWEEKLY,
            limit = 50_000L,
            spentByStart = mapOf(
                LocalDate.of(2026, 1, 1) to 20_000L,
                LocalDate.of(2026, 1, 16) to 30_000L,
                LocalDate.of(2026, 2, 1) to 10_000L,
            ),
        )

        // 1–15 ene: 50 - 20 = 30. 16–31 ene: 30 + 50 - 30 = 50.
        // 1–15 feb: 50 + 50 - 10 = 90.
        assertEquals(90_000L, result)
    }

    @Test
    fun `un limite cero no arrastra`() {
        val today = LocalDate.of(2026, 3, 10)
        val created = LocalDate.of(2026, 1, 1)

        val result = carry(today, created, limit = 0L)

        assertEquals(0L, result)
    }
}
