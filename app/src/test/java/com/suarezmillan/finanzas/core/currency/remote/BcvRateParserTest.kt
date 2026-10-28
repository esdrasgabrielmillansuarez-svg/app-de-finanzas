package com.suarezmillan.finanzas.core.currency.remote

import com.suarezmillan.finanzas.core.currency.LiveRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BcvRateParserTest {

    private val officialJson = """
        {
          "moneda": "USD",
          "fuente": "oficial",
          "nombre": "Dólar",
          "compra": null,
          "venta": null,
          "promedio": 888.1183,
          "fechaActualizacion": "2026-10-28T00:00:00-04:00"
        }
    """.trimIndent()

    @Test
    fun `parsea el promedio oficial del BCV`() {
        val rate = BcvRateParser.parse(officialJson, fetchedAtMillis = 123L)

        assertEquals(888.1183, rate!!.unitsPerUsd, 1e-9)
        assertEquals(123L, rate.fetchedAtMillis)
        assertEquals("BCV", rate.source)
    }

    @Test
    fun `parsea el paralelo con su etiqueta`() {
        val json = """{"moneda":"USD","fuente":"paralelo","promedio":1004.02693}"""

        val rate = BcvRateParser.parse(
            json = json,
            expectedSource = "paralelo",
            sourceLabel = "Paralelo",
            fetchedAtMillis = 9L,
        )

        assertEquals(1_004.02693, rate!!.unitsPerUsd, 1e-9)
        assertEquals("Paralelo", rate.source)
        assertEquals(9L, rate.fetchedAtMillis)
    }

    @Test
    fun `rechaza cualquier fuente que no sea la esperada`() {
        val paralelo = officialJson.replace("\"oficial\"", "\"paralelo\"")

        assertNull(BcvRateParser.parse(paralelo))
        assertNull(
            BcvRateParser.parse(
                officialJson,
                expectedSource = "paralelo",
                sourceLabel = "Paralelo",
            ),
        )
    }

    @Test
    fun `usa venta si promedio viene vacio`() {
        val json = """{"fuente":"oficial","promedio":null,"venta":912.5}"""

        assertEquals(912.5, BcvRateParser.parse(json)!!.unitsPerUsd, 1e-9)
    }

    @Test
    fun `descarta json invalido o valores no positivos`() {
        assertNull(BcvRateParser.parse("no es json"))
        assertNull(BcvRateParser.parse("""{"fuente":"oficial","promedio":0}"""))
        assertNull(BcvRateParser.parse("""{"fuente":"oficial","promedio":-5}"""))
    }

    @Test
    fun `la cache de seis horas decide si vale la pena pedir de nuevo`() {
        val now = 1_000_000_000_000L

        assertTrue(ExchangeRateUpdater.isFreshEnough(now, now))
        assertTrue(ExchangeRateUpdater.isFreshEnough(now - 5 * 60 * 60 * 1000L, now))
        assertFalse(ExchangeRateUpdater.isFreshEnough(now - 7 * 60 * 60 * 1000L, now))
        // Reloj hacia atrás: mejor volver a pedir que confiar en el futuro.
        assertFalse(ExchangeRateUpdater.isFreshEnough(now + 60_000L, now))
    }

    @Test
    fun `LiveRate descarta valores no finitos`() {
        assertTrue(LiveRate(888.1183, 0L, "BCV").isFresh)
        assertFalse(LiveRate(0.0, 0L, "BCV").isFresh)
        assertFalse(LiveRate(Double.NaN, 0L, "BCV").isFresh)
        assertFalse(LiveRate(Double.POSITIVE_INFINITY, 0L, "BCV").isFresh)
    }
}
