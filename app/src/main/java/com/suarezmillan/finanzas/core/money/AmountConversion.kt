package com.suarezmillan.finanzas.core.money

import kotlin.math.roundToLong

/**
 * Convierte un importe respetando la tasa histórica del movimiento.
 *
 * Un gasto en USD conserva la tasa que se usó al registrarlo. Solo se usa esa
 * tasa si la divisa destino es exactamente la base con la que se guardó; si la
 * base cambió (o el movimiento es anterior a la v4), se consulta la tasa actual.
 *
 * Devuelve `null` cuando no hay ninguna tasa real disponible: nunca se inventa
 * una equivalencia.
 */
fun convertAmount(
    amountMinor: Long,
    from: String,
    to: String,
    historicalRate: Double?,
    historicalBase: String?,
    currentRate: (from: String, to: String) -> Double?,
): Long? {
    if (from.trim().equals(to.trim(), ignoreCase = true)) return amountMinor

    val storedRate = historicalRate
        ?.takeIf { it > 0.0 && it.isFinite() && historicalBase.equals(to, ignoreCase = true) }

    val rate = storedRate ?: currentRate(from, to) ?: return null
    return (amountMinor * rate).roundToLong()
}
