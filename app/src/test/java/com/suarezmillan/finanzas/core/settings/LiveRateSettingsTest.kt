package com.suarezmillan.finanzas.core.settings

import com.suarezmillan.finanzas.core.currency.LiveRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveRateSettingsTest {

    @Test
    fun `la cotizacion BCV sobrevive al reinicio`() {
        val store = InMemoryPreferenceStore()
        val first = AppSettings(store)

        first.setLiveRate(LiveRate(unitsPerUsd = 888.1183, fetchedAtMillis = 42L, source = "BCV"))

        val reloaded = AppSettings(store).liveRate.value!!
        assertEquals(888.1183, reloaded.unitsPerUsd, 1e-12)
        assertEquals(42L, reloaded.fetchedAtMillis)
        assertEquals("BCV", reloaded.source)
    }

    @Test
    fun `no guarda cotizaciones invalidas`() {
        val settings = AppSettings(InMemoryPreferenceStore())

        settings.setLiveRate(LiveRate(unitsPerUsd = 0.0, fetchedAtMillis = 1L, source = "BCV"))

        assertNull(settings.liveRate.value)
    }
}
