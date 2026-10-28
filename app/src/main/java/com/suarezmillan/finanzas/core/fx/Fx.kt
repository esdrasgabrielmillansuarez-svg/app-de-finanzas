package com.suarezmillan.finanzas.core.fx

import kotlin.math.roundToLong

/**
 * Origen de una tasa de cambio.
 *
 * Se persiste por transacción: cambiar la tasa de hoy **no** reescribe el pasado.
 */
enum class FxSource {
    /** La transacción está en la divisa base: no hay conversión. */
    NONE,

    /** Tasa obtenida automáticamente (manual del usuario o fuente externa cuando exista). */
    AUTO,

    /** Tasa manual puntual para esa transacción. */
    MANUAL,

    /** Tasa fija configurada por el usuario para todas sus conversiones. */
    FIXED,
}

/**
 * Una tasa y su procedencia.
 *
 * `rate` expresa cuántas unidades de [quote] equivalen a 1 unidad de [base].
 * Ej.: base = "USD", quote = "ARS", rate = 4120.50.
 */
data class FxRate(
    val base: String,
    val quote: String,
    val rate: Double,
    val source: FxSource,
    val timestampMillis: Long,
) {
    init {
        require(rate > 0) { "La tasa debe ser positiva" }
    }

    /** Convierte un importe de [base] a [quote]. Redondeo a centavo. */
    fun convertToQuote(minorInBase: Long): Long = (minorInBase * rate).roundToLong()

    /** Inversa. */
    fun convertToBase(minorInQuote: Long): Long = (minorInQuote / rate).roundToLong()
}

object FxMath {
    fun convert(minor: Long, rate: Double): Long = (minor * rate).roundToLong()
}
