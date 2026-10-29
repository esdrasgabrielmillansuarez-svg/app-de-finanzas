package com.suarezmillan.finanzas.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateBudgetTest {

    @Test
    fun `el ingreso suma al disponible del mes`() {
        val onlySpent = HomeUiState(spentMinor = 25_000L)
        val withIncome = HomeUiState(incomeMinor = 100_000L, spentMinor = 25_000L)

        assertEquals(-25_000L, onlySpent.remainingMinor)
        assertEquals(75_000L, withIncome.remainingMinor)
        assertTrue(withIncome.hasIncome)
        assertTrue(withIncome.hasMonthActivity)
    }

    @Test
    fun `el gasto resta del ingreso`() {
        val before = HomeUiState(incomeMinor = 200_000L, spentMinor = 50_000L)
        val after = before.copy(spentMinor = 80_000L)

        assertEquals(150_000L, before.remainingMinor)
        assertEquals(120_000L, after.remainingMinor)
        assertTrue(after.remainingMinor < before.remainingMinor)
    }

    @Test
    fun `el limite de sobres no se suma al disponible`() {
        val state = HomeUiState(
            incomeMinor = 100_000L,
            spentMinor = 30_000L,
            budgetLimitMinor = 500_000L,
        )

        assertEquals(70_000L, state.remainingMinor)
    }

    @Test
    fun `sin ingreso no hay porcentaje de ingreso gastado`() {
        val state = HomeUiState(spentMinor = 50_000L)

        assertEquals(0f, state.usedRatio, 0.0001f)
        assertFalse(state.hasIncome)
    }
}
