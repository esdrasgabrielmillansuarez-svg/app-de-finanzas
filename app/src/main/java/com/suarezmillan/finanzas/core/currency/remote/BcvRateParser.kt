package com.suarezmillan.finanzas.core.currency.remote

import com.suarezmillan.finanzas.core.currency.LiveRate
import org.json.JSONObject

/**
 * Parser del JSON que publica el espejo del BCV:
 *
 * ```json
 * { "moneda":"USD", "fuente":"oficial", "promedio":888.1183, ... }
 * ```
 *
 * Reglas deliberadas:
 * - Solo aceptamos `fuente = "oficial"`. Si el día de mañana el endpoint
 *   devuelve el paralelo, preferimos no actualizar antes que mostrar un dólar
 *   que no es el del BCV.
 * - `promedio` manda; si viniera vacío se prueban `venta` y `compra`.
 * - Cualquier valor ≤ 0, NaN o infinito se descarta.
 */
object BcvRateParser {

    fun parse(json: String, fetchedAtMillis: Long = System.currentTimeMillis()): LiveRate? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null

        val source = obj.optString("fuente", "")
        if (source.isNotBlank() && !source.equals("oficial", ignoreCase = true)) return null

        val value = sequenceOf("promedio", "venta", "compra")
            .map { key -> obj.optDouble(key, Double.NaN) }
            .firstOrNull { it > 0.0 && it.isFinite() }
            ?: return null

        return LiveRate(
            unitsPerUsd = value,
            fetchedAtMillis = fetchedAtMillis,
            source = "BCV",
        )
    }
}
