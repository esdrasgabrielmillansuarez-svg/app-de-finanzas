package com.suarezmillan.finanzas.core.currency

import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.core.settings.AppSettings
import com.suarezmillan.finanzas.core.settings.InMemoryPreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyRatesTest {

    private fun rates() = CurrencyRates(AppSettings(InMemoryPreferenceStore()))

    @Test
    fun `pivote por dolar`() {
        val rates = rates()

        assertEquals(1_050.0, rates.rate("USD", "ARS"), 0.0001)
        assertEquals(1.0 / 1_050.0, rates.rate("ARS", "USD"), 1e-9)
        assertEquals(1.0, rates.rate("ARS", "ARS"), 1e-9)
        assertEquals(0.92, rates.rate("USD", "EUR"), 0.0001)
    }

    @Test
    fun `convierte importes sin perder centavos`() {
        val rates = rates()

        // 105.000,00 ARS = 100,00 USD con la referencia 1.050
        assertEquals(10_000L, rates.convert(10_500_000L, "ARS", "USD"))
        assertEquals(10_500_000L, rates.convert(10_000L, "USD", "ARS"))
    }

    @Test
    fun `equivalencias en dolar y euro por unidad`() {
        val rates = rates()
        val ars = rates.quote("ARS")

        assertEquals(1.0 / 1_050.0, ars.usdPerUnit, 1e-9)
        assertEquals((1.0 / 1_050.0) * 0.92, ars.eurPerUnit, 1e-9)
        assertFalse(ars.isManual)
    }

    @Test
    fun `una tasa manual manda sobre la referencia`() {
        val settings = AppSettings(InMemoryPreferenceStore())
        val rates = CurrencyRates(settings)

        settings.setManualRate("ARS", 2_000.0)

        assertEquals(2_000.0, rates.rate("USD", "ARS"), 0.0001)
        assertTrue(rates.quote("ARS").isManual)

        settings.clearManualRate("ARS")
        assertFalse(rates.quote("ARS").isManual)
        assertEquals(1_050.0, rates.rate("USD", "ARS"), 0.0001)
    }

    @Test
    fun `catalogo cubre paises y busca por pais`() {
        assertTrue(CurrencyCatalog.all.size >= 40)
        assertTrue(CurrencyCatalog.search("españa").any { it.code == "EUR" })
        assertTrue(CurrencyCatalog.search("argentina").any { it.code == "ARS" })
        assertTrue(CurrencyCatalog.search("jap").any { it.code == "JPY" })
        assertTrue(CurrencyCatalog.search("").size == CurrencyCatalog.all.size)
    }

    @Test
    fun `formato de tasas`() {
        assertEquals("1.050", MoneyFormat.formatRate(1_050.0))
        assertEquals("0,92", MoneyFormat.formatRate(0.92))
        assertEquals("0,000876", MoneyFormat.formatRate(0.000876))
        assertEquals("—", MoneyFormat.formatRate(0.0))
    }
}
