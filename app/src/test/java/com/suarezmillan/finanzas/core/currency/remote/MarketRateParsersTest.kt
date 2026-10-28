package com.suarezmillan.finanzas.core.currency.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketRateParsersTest {

    @Test
    fun `el euro de Frankfurter ya viene en euros por dolar`() {
        val json = """{"amount":1.0,"base":"USD","date":"2026-10-28","rates":{"EUR":0.89823}}"""

        val rate = EurRateParser.parse(json, fetchedAtMillis = 5L)!!

        assertEquals(0.89823, rate.unitsPerUsd, 1e-9)
        assertEquals(5L, rate.fetchedAtMillis)
        assertEquals("BCE/Frankfurter", rate.source)
    }

    @Test
    fun `el peso argentino usa la venta oficial`() {
        val json = """
            {
              "moneda": "USD",
              "casa": "oficial",
              "nombre": "Oficial",
              "compra": 1510,
              "venta": 1560,
              "fechaActualizacion": "2026-10-28T18:00:00.000Z"
            }
        """.trimIndent()

        val rate = ArsRateParser.parse(json)!!

        assertEquals(1_560.0, rate.unitsPerUsd, 1e-9)
        assertEquals("ARS oficial (venta)", rate.source)
    }

    @Test
    fun `si falta la venta usa la compra`() {
        val json = """{"casa":"oficial","compra":1510,"venta":null}"""

        val rate = ArsRateParser.parse(json)!!

        assertEquals(1_510.0, rate.unitsPerUsd, 1e-9)
        assertEquals("ARS oficial (compra)", rate.source)
    }

    @Test
    fun `rechaza el blue y json invalido`() {
        assertNull(ArsRateParser.parse("""{"casa":"blue","venta":1565}"""))
        assertNull(ArsRateParser.parse("""{"casa":"oficial"}"""))
        assertNull(ArsRateParser.parse("no es json"))
        assertNull(EurRateParser.parse("""{"rates":{}}"""))
        assertNull(EurRateParser.parse("""{"rates":{"EUR":0}}"""))
    }

    @Test
    fun `la tabla global solo devuelve divisas conocidas y validas`() {
        val json = """
            {
              "result": "success",
              "base_code": "USD",
              "rates": {
                "EUR": 0.897453,
                "ARS": 1538.741,
                "BOB": 11.855191,
                "VES": 888.0,
                "USD": 1.0,
                "USDT": 1.0,
                "JPY": 0
              }
            }
        """.trimIndent()

        val rates = BulkRateParser.parse(
            json,
            codes = setOf("EUR", "ARS", "BOB", "VES", "USD", "USDT", "JPY", "XYZ"),
            fetchedAtMillis = 9L,
        )

        assertEquals(setOf("EUR", "ARS", "BOB"), rates.keys)
        assertEquals(0.897453, rates.getValue("EUR").unitsPerUsd, 1e-9)
        assertEquals(1_538.741, rates.getValue("ARS").unitsPerUsd, 1e-9)
        assertEquals(9L, rates.getValue("EUR").fetchedAtMillis)
        assertEquals("ExchangeRate-API", rates.getValue("BOB").source)
    }

    @Test
    fun `la tabla global rechaza respuestas invalidas`() {
        assertTrue(BulkRateParser.parse("no es json", setOf("EUR")).isEmpty())
        assertTrue(BulkRateParser.parse("""{"result":"error"}""", setOf("EUR")).isEmpty())
        assertTrue(BulkRateParser.parse("""{"result":"success","rates":{}}""", setOf("EUR")).isEmpty())
    }
}
