package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.core.currency.CurrencyRates
import com.suarezmillan.finanzas.core.money.convertAmount
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import kotlin.math.abs

/**
 * Importe absoluto del movimiento expresado en [target].
 *
 * Los gastos se guardan negativos; para presupuestos y totales se usa su valor
 * absoluto. Si no hay tasa, devuelve `null` y el llamador decide cómo avisar.
 */
internal fun TxnEntity.absoluteAmountIn(
    target: String,
    rates: CurrencyRates,
): Long? = convertAmount(
    amountMinor = abs(amountMinor),
    from = currency,
    to = target,
    historicalRate = fxRateToBase,
    historicalBase = fxBaseCurrency,
    currentRate = rates::rate,
)
