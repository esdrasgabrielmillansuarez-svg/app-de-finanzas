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
    fun `la referencia BCV manda sobre la tabla offline`() {
        val settings = AppSettings(InMemoryPreferenceStore())
        val rates = CurrencyRates(settings)
        val now = System.currentTimeMillis()

        settings.setVesReferences(mapOf(VesReference.BCV to LiveRate(900.0, now, "BCV")))

        assertEquals(900.0, rates.rate("USD", "VES"), 1e-9)
        val ves = rates.quote("VES")
        assertTrue(ves.isLive)
        assertEquals("BCV", ves.liveSource)
        assertEquals(now, ves.liveUpdatedAtMillis)
        assertFalse(ves.isManual)
        // El euro sigue derivándose del pivote USD.
        assertEquals(900.0 / 0.92, rates.rate("EUR", "VES"), 0.0001)
    }

    @Test
    fun `se puede cambiar entre BCV paralelo y USDT`() {
        val settings = AppSettings(InMemoryPreferenceStore())
        val rates = CurrencyRates(settings)
        settings.setVesReferences(
            mapOf(
                VesReference.BCV to LiveRate(900.0, 1L, "BCV"),
                VesReference.PARALELO to LiveRate(1_000.0, 2L, "Paralelo"),
                VesReference.USDT to LiveRate(1_010.0, 3L, "USDT (paralelo)"),
            ),
        )

        assertEquals(VesReference.BCV, settings.activeVesReference.value)
        assertEquals(900.0, rates.rate("USD", "VES"), 1e-9)

        settings.setActiveVesReference(VesReference.PARALELO)
        assertEquals(1_000.0, rates.rate("USD", "VES"), 1e-9)

        settings.setActiveVesReference(VesReference.USDT)
        assertEquals(1_010.0, rates.rate("USD", "VES"), 1e-9)
        assertEquals("USDT (paralelo)", rates.quote("VES").liveSource)
    }

    @Test
    fun `cada referencia del bolivar tiene su propia tasa manual`() {
        val settings = AppSettings(InMemoryPreferenceStore())
        val rates = CurrencyRates(settings)
        settings.setVesReferences(
            mapOf(
                VesReference.BCV to LiveRate(900.0, 1L, "BCV"),
                VesReference.PARALELO to LiveRate(1_000.0, 2L, "Paralelo"),
            ),
        )

        settings.setActiveVesReference(VesReference.PARALELO)
        settings.setVesManualRate(VesReference.PARALELO, 1_100.0)

        assertEquals(1_100.0, rates.rate("USD", "VES"), 1e-9)
        assertTrue(rates.quote("VES").isManual)
        assertFalse(rates.quote("VES").isLive)

        // BCV conserva su referencia: el override no se filtra entre referencias.
        settings.setActiveVesReference(VesReference.BCV)
        assertEquals(900.0, rates.rate("USD", "VES"), 1e-9)
        assertFalse(rates.quote("VES").isManual)

        // La API genérica de VES escribe sobre la referencia activa.
        settings.setManualRate("VES", 950.0)
        assertEquals(950.0, rates.rate("USD", "VES"), 1e-9)

        settings.clearManualRate("VES")
        assertEquals(900.0, rates.rate("USD", "VES"), 1e-9)
    }

    @Test
    fun `euro y peso argentino usan su referencia en vivo`() {
        val settings = AppSettings(InMemoryPreferenceStore())
        val rates = CurrencyRates(settings)
        val now = System.currentTimeMillis()
        settings.setLiveRates(
            mapOf(
                "EUR" to LiveRate(0.89823, now, "BCE/Frankfurter"),
                "ARS" to LiveRate(1_560.0, now, "ARS oficial (venta)"),
            ),
        )

        assertEquals(0.89823, rates.quote("EUR").unitsPerUsd, 1e-9)
        assertEquals(1_560.0, rates.rate("USD", "ARS"), 1e-9)
        assertTrue(rates.quote("EUR").isLive)
        assertTrue(rates.quote("ARS").isLive)

        settings.setManualRate("EUR", 0.95)
        assertEquals(0.95, rates.quote("EUR").unitsPerUsd, 1e-9)
        assertFalse(rates.quote("EUR").isLive)
    }

    @Test
    fun `USDT existe como divisa y vale un dolar`() {
        val settings = AppSettings(InMemoryPreferenceStore())
        val rates = CurrencyRates(settings)
        settings.setVesReferences(mapOf(VesReference.BCV to LiveRate(888.0, 1L, "BCV")))

        val usdt = rates.quote("USDT")
        assertEquals(1.0, usdt.usdPerUnit, 1e-9)
        // 1 USDT ≈ 1 USD → con BCV 888, un USDT son 888 VES.
        assertEquals(888.0, rates.rate("USDT", "VES"), 1e-9)
    }

    @Test
    fun `sin red la referencia del bolivar sigue siendo la ultima conocida`() {
        val settings = AppSettings(InMemoryPreferenceStore())
        val rates = CurrencyRates(settings)

        // Recién instalada y sin internet: la tabla de arranque, no un cero.
        assertEquals(CurrencyCatalog.unitsPerUsd("VES"), rates.rate("USD", "VES"), 1e-9)
        assertFalse(rates.quote("VES").isLive)
    }

    @Test
    fun `catalogo cubre paises y busca por pais`() {
        assertTrue(CurrencyCatalog.all.size >= 40)
        assertTrue(CurrencyCatalog.search("españa").any { it.code == "EUR" })
        assertTrue(CurrencyCatalog.search("argentina").any { it.code == "ARS" })
        assertTrue(CurrencyCatalog.search("jap").any { it.code == "JPY" })
        assertTrue(CurrencyCatalog.search("venezuela").any { it.code == "USDT" })
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
