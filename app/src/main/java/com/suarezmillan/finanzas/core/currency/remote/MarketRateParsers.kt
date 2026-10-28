package com.suarezmillan.finanzas.core.currency.remote

import com.suarezmillan.finanzas.core.currency.LiveRate
import org.json.JSONObject

/**
 * Tabla global de respaldo: ExchangeRate-API publica todas sus divisas en una
 * sola respuesta. Solo se usan los códigos que existen en el catálogo de la app
 * y se descartan valores vacíos o no finitos.
 *
 * ```json
 * { "result":"success", "base_code":"USD", "rates": { "EUR":0.897453, "ARS":1538.741 } }
 * ```
 */
object BulkRateParser {

    fun parse(
        json: String,
        codes: Set<String>,
        fetchedAtMillis: Long = System.currentTimeMillis(),
    ): Map<String, LiveRate> {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return emptyMap()
        if (obj.optString("result").equals("error", ignoreCase = true)) return emptyMap()
        val rates = obj.optJSONObject("rates") ?: return emptyMap()

        return buildMap {
            codes.forEach { code ->
                val normalized = code.trim().uppercase()
                if (normalized == "VES" || normalized == "USD" || normalized == "USDT") return@forEach
                val value = rates.optDouble(normalized, Double.NaN)
                if (value > 0.0 && value.isFinite()) {
                    put(
                        normalized,
                        LiveRate(
                            unitsPerUsd = value,
                            fetchedAtMillis = fetchedAtMillis,
                            source = "ExchangeRate-API",
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Euro global: Frankfurter publica el tipo del BCE.
 *
 * ```json
 * { "base":"USD", "rates": { "EUR": 0.89823 } }
 * ```
 *
 * `rates.EUR` ya es "euros por 1 USD", justo lo que guarda [LiveRate].
 */
object EurRateParser {

    fun parse(json: String, fetchedAtMillis: Long = System.currentTimeMillis()): LiveRate? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val value = obj.optJSONObject("rates")?.optDouble("EUR", Double.NaN) ?: return null
        if (value <= 0.0 || !value.isFinite()) return null
        return LiveRate(
            unitsPerUsd = value,
            fetchedAtMillis = fetchedAtMillis,
            source = "BCE/Frankfurter",
        )
    }
}

/**
 * Peso argentino oficial. dolarapi publica compra/venta; usamos **venta**
 * (lo que cuesta comprar un dólar) y, si falta, compra.
 *
 * ```json
 * { "casa":"oficial", "compra":1510, "venta":1560, ... }
 * ```
 */
object ArsRateParser {

    fun parse(json: String, fetchedAtMillis: Long = System.currentTimeMillis()): LiveRate? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null

        val casa = obj.optString("casa", "oficial")
        if (casa.isNotBlank() && !casa.equals("oficial", ignoreCase = true)) return null

        val venta = obj.optDouble("venta", Double.NaN)
        val compra = obj.optDouble("compra", Double.NaN)
        val useVenta = venta > 0.0 && venta.isFinite()
        val value = when {
            useVenta -> venta
            compra > 0.0 && compra.isFinite() -> compra
            else -> return null
        }

        return LiveRate(
            unitsPerUsd = value,
            fetchedAtMillis = fetchedAtMillis,
            source = if (useVenta) "ARS oficial (venta)" else "ARS oficial (compra)",
        )
    }
}
