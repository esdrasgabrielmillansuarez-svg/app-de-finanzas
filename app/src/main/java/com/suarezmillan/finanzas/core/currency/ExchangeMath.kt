package com.suarezmillan.finanzas.core.currency

import kotlin.math.roundToLong

/**
 * Aritmética de la calculadora de cambio.
 *
 * Vive fuera de la UI para poder probarla en la JVM. No conoce divisas ni
 * fuentes: recibe la tasa ya resuelta por [CurrencyRates]. Si falta la tasa,
 * devuelve `null` en vez de inventar una equivalencia.
 */
object ExchangeMath {

    /** Convierte [amountMinor] con [rate]; `null` si falta el monto o la tasa. */
    fun convert(amountMinor: Long?, rate: Double?): Long? {
        if (amountMinor == null || amountMinor <= 0L) return null
        if (rate == null || !rate.isFinite() || rate <= 0.0) return null
        return (amountMinor * rate).roundToLong()
    }
}
