package com.suarezmillan.finanzas.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class AmountConversionTest {

    @Test
    fun `la misma divisa no necesita cotizacion`() {
        val currentRateCalled = AtomicBoolean(false)

        val result = convertAmount(
            amountMinor = 12_345L,
            from = "VES",
            to = "VES",
            historicalRate = null,
            historicalBase = null,
            currentRate = { _, _ ->
                currentRateCalled.set(true)
                null
            },
        )

        assertEquals(12_345L, result)
        assertFalse(currentRateCalled.get())
    }

    @Test
    fun `usa la tasa historica cuando la base guardada coincide`() {
        val currentRateCalled = AtomicBoolean(false)

        // 100,00 USD a 1.100 VES/USD = 110.000,00 VES
        val result = convertAmount(
            amountMinor = 10_000L,
            from = "USD",
            to = "VES",
            historicalRate = 1_100.0,
            historicalBase = "VES",
            currentRate = { _, _ ->
                currentRateCalled.set(true)
                null
            },
        )

        assertEquals(11_000_000L, result)
        assertFalse(currentRateCalled.get())
    }

    @Test
    fun `cae a la tasa actual si la base cambio`() {
        val result = convertAmount(
            amountMinor = 10_000L,
            from = "USD",
            to = "VES",
            historicalRate = 1_100.0,
            historicalBase = "EUR",
            currentRate = { from, to ->
                assertEquals("USD", from)
                assertEquals("VES", to)
                2_000.0
            },
        )

        assertEquals(20_000_000L, result)
    }

    @Test
    fun `las filas antiguas sin base usan la tasa actual`() {
        val result = convertAmount(
            amountMinor = 10_000L,
            from = "USD",
            to = "VES",
            historicalRate = 1_100.0,
            historicalBase = null,
            currentRate = { _, _ -> 1_500.0 },
        )

        assertEquals(15_000_000L, result)
    }

    @Test
    fun `sin ninguna tasa devuelve null y no inventa`() {
        assertNull(
            convertAmount(
                amountMinor = 10_000L,
                from = "USD",
                to = "VES",
                historicalRate = null,
                historicalBase = null,
                currentRate = { _, _ -> null },
            ),
        )
    }

    @Test
    fun `una tasa historica invalida no se usa`() {
        val result = convertAmount(
            amountMinor = 10_000L,
            from = "USD",
            to = "VES",
            historicalRate = Double.NaN,
            historicalBase = "VES",
            currentRate = { _, _ -> 900.0 },
        )

        assertEquals(9_000_000L, result)
    }
}
