package com.suarezmillan.finanzas.core.currency

/**
 * Cotización obtenida desde una fuente externa (hoy, el dólar oficial del BCV).
 *
 * Se guarda completa para poder seguir mostrando el último valor conocido
 * aunque no haya red, y para decir con honestidad cuándo se actualizó.
 */
data class LiveRate(
    /** Unidades de la divisa por 1 USD (para VES: bolívares por dólar). */
    val unitsPerUsd: Double,
    /** Momento en que la app obtuvo el dato (epoch millis). */
    val fetchedAtMillis: Long,
    /** Etiqueta de la fuente, p. ej. "BCV". */
    val source: String,
) {
    val isFresh: Boolean
        get() = unitsPerUsd > 0.0 && !unitsPerUsd.isNaN() && !unitsPerUsd.isInfinite()
}
