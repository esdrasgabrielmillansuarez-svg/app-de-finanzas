package com.suarezmillan.finanzas.core.currency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExchangeMathTest {

    @Test
    fun `convierte bolivares a dolares con la tasa activa`() {
        // 1 USD = 889,26 VES: Bs. 8.892,60 son US$ 10,00.
        assertEquals(1_000L, ExchangeMath.convert(889_260L, 1.0 / 889.26))
    }

    @Test
    fun `convierte dolares a bolivares sin perder centavos`() {
        assertEquals(889_260L, ExchangeMath.convert(1_000L, 889.26))
    }

    @Test
    fun `sin tasa no se inventa una equivalencia`() {
        assertNull(ExchangeMath.convert(1_000L, null))
        assertNull(ExchangeMath.convert(1_000L, 0.0))
        assertNull(ExchangeMath.convert(1_000L, -5.0))
        assertNull(ExchangeMath.convert(1_000L, Double.NaN))
        assertNull(ExchangeMath.convert(1_000L, Double.POSITIVE_INFINITY))
    }

    @Test
    fun `sin monto no hay resultado`() {
        assertNull(ExchangeMath.convert(null, 889.26))
        assertNull(ExchangeMath.convert(0L, 889.26))
        assertNull(ExchangeMath.convert(-100L, 889.26))
    }

    @Test
    fun `redondea al centavo mas cercano`() {
        assertEquals(2L, ExchangeMath.convert(1L, 1.5))
        assertEquals(1L, ExchangeMath.convert(1L, 1.4))
    }
}
