package com.suarezmillan.finanzas.core.settings

import com.suarezmillan.finanzas.core.currency.FetchedRates
import com.suarezmillan.finanzas.core.currency.LiveRate
import com.suarezmillan.finanzas.core.currency.VesReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRateSettingsTest {

    @Test
    fun `las referencias y la eleccion sobreviven al reinicio`() {
        val store = InMemoryPreferenceStore()
        val first = AppSettings(store)

        first.setVesReferences(
            mapOf(
                VesReference.BCV to LiveRate(888.1183, 42L, "BCV"),
                VesReference.PARALELO to LiveRate(1_004.02693, 43L, "Paralelo"),
                VesReference.USDT to LiveRate(1_004.02693, 43L, "USDT (paralelo)"),
            ),
        )
        first.setActiveVesReference(VesReference.PARALELO)
        first.setVesManualRate(VesReference.USDT, 1_200.0)
        first.setLiveRates(
            mapOf(
                "EUR" to LiveRate(0.89823, 44L, "BCE/Frankfurter"),
                "ARS" to LiveRate(1_560.0, 45L, "ARS oficial (venta)"),
            ),
        )
        first.setManualRate("EUR", 0.95)

        val reloaded = AppSettings(store)

        assertEquals(3, reloaded.vesReferences.value.size)
        assertEquals(VesReference.PARALELO, reloaded.activeVesReference.value)
        assertEquals(1_004.02693, reloaded.activeVesRate()!!.unitsPerUsd, 1e-9)
        assertEquals(43L, reloaded.activeVesRate()!!.fetchedAtMillis)
        assertEquals(1_200.0, reloaded.vesManualRates.value[VesReference.USDT]!!, 1e-9)
        assertEquals(0.89823, reloaded.liveRates.value["EUR"]!!.unitsPerUsd, 1e-9)
        assertEquals(1_560.0, reloaded.liveRates.value["ARS"]!!.unitsPerUsd, 1e-9)
        assertEquals(0.95, reloaded.manualUnitsPerUsd.value["EUR"]!!, 1e-9)
        assertTrue(reloaded.isManual("EUR"))
    }

    @Test
    fun `no guarda tasas invalidas`() {
        val settings = AppSettings(InMemoryPreferenceStore())

        settings.setVesReferences(
            mapOf(
                VesReference.BCV to LiveRate(0.0, 1L, "BCV"),
                VesReference.PARALELO to LiveRate(Double.NaN, 1L, "Paralelo"),
            ),
        )
        settings.setLiveRates(mapOf("EUR" to LiveRate(Double.POSITIVE_INFINITY, 1L, "BCE")))
        settings.setVesManualRate(VesReference.BCV, 0.0)

        assertTrue(settings.vesReferences.value.isEmpty())
        assertTrue(settings.liveRates.value.isEmpty())
        assertTrue(settings.vesManualRates.value.isEmpty())
        assertNull(settings.activeVesRate())
    }

    @Test
    fun `quitar una tasa manual la borra del almacenamiento`() {
        val store = InMemoryPreferenceStore()
        val settings = AppSettings(store)

        settings.setVesManualRate(VesReference.BCV, 900.0)
        settings.setManualRate("ARS", 1_600.0)
        assertTrue(settings.isManual("VES"))
        assertTrue(settings.isManual("ARS"))

        settings.clearVesManualRate(VesReference.BCV)
        settings.clearManualRate("ARS")

        assertFalse(settings.isManual("VES"))
        assertFalse(settings.isManual("ARS"))
        val reloaded = AppSettings(store)
        assertTrue(reloaded.vesManualRates.value.isEmpty())
        assertTrue(reloaded.manualUnitsPerUsd.value.isEmpty())
    }

    @Test
    fun `updateRates guarda resultados parciales y conserva los previos`() {
        val settings = AppSettings(InMemoryPreferenceStore())

        settings.updateRates(
            FetchedRates(
                ves = mapOf(
                    VesReference.BCV to LiveRate(888.0, 1L, "BCV"),
                    VesReference.PARALELO to LiveRate(1_004.0, 2L, "Paralelo"),
                    VesReference.USDT to LiveRate(1_004.0, 2L, "USDT (paralelo)"),
                ),
                currencies = mapOf("EUR" to LiveRate(0.9, 3L, "BCE")),
            ),
        )

        assertEquals(setOf("VES", "EUR"), settings.allLiveRates().keys)

        // Llega solo ARS: EUR y VES se conservan.
        settings.updateRates(
            FetchedRates(currencies = mapOf("ARS" to LiveRate(1_560.0, 4L, "ARS oficial"))),
        )

        assertEquals(1_560.0, settings.liveRates.value["ARS"]!!.unitsPerUsd, 1e-9)
        assertEquals(0.9, settings.liveRates.value["EUR"]!!.unitsPerUsd, 1e-9)
        assertEquals(888.0, settings.activeVesRate()!!.unitsPerUsd, 1e-9)
    }

    @Test
    fun `migra la cotizacion BCV de la version anterior`() {
        val store = InMemoryPreferenceStore()
        store.putDouble("live_ves_units_per_usd", 888.0)
        store.putLong("live_ves_fetched_at", 7L)
        store.putString("live_ves_source", "BCV")

        val settings = AppSettings(store)
        val migrated = settings.vesReferences.value[VesReference.BCV]!!

        assertEquals(888.0, migrated.unitsPerUsd, 1e-9)
        assertEquals(7L, migrated.fetchedAtMillis)
        assertEquals("BCV", migrated.source)
    }
}
