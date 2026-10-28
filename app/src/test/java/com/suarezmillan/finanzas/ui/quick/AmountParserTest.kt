package com.suarezmillan.finanzas.ui.quick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** El parser del teclado es dinero: si falla, guarda importes equivocados. */
class AmountParserTest {

    @Test
    fun `coma decimal`() {
        assertEquals(1250L, AmountParser.evaluateToMinor("12,50"))
        assertEquals(1250L, AmountParser.evaluateToMinor("12.50"))
    }

    @Test
    fun `aritmetica inline`() {
        assertEquals(46200L, AmountParser.evaluateToMinor("12+450"))
        assertEquals(60000L, AmountParser.evaluateToMinor("1200/2"))
        assertEquals(29850L, AmountParser.evaluateToMinor("3*99,5"))
        assertEquals(10000L, AmountParser.evaluateToMinor("120-20"))
    }

    @Test
    fun `precedencia`() {
        assertEquals(5000L, AmountParser.evaluateToMinor("10+10*4"))
    }

    @Test
    fun `entradas invalidas devuelven null`() {
        assertNull(AmountParser.evaluateToMinor(""))
        assertNull(AmountParser.evaluateToMinor("12+"))
        assertNull(AmountParser.evaluateToMinor("abc"))
        assertNull(AmountParser.evaluateToMinor("1/0"))
        assertNull(AmountParser.evaluateToMinor("0"))
        assertNull(AmountParser.evaluateToMinor("-5"))
    }
}
