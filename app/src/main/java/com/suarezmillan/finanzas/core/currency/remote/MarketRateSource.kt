package com.suarezmillan.finanzas.core.currency.remote

import com.suarezmillan.finanzas.core.branding.Branding
import com.suarezmillan.finanzas.core.currency.CurrencyCatalog
import com.suarezmillan.finanzas.core.currency.FetchedRates
import com.suarezmillan.finanzas.core.currency.VesReference
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Referencias públicas, en paralelo y con timeout corto:
 *
 * - **BCV oficial** y **paralelo** desde el espejo `ve.dolarapi.com`.
 * - **Euro** desde Frankfurter (tipo del BCE).
 * - **ARS oficial** desde dolarapi Argentina.
 * - **Tabla global** desde ExchangeRate-API para el resto del catálogo.
 *
 * USDT no tiene una cotización pública estable en VES: se estima con el
 * paralelo y el usuario puede fijar otra equivalencia a mano. La app no envía
 * datos: son GET anónimos.
 *
 * TODO si aparece un endpoint estable del BCV o de USDT/VES: cambiar solo esta clase.
 */
class MarketRateSource @Inject constructor() : RateSource {

    override suspend fun fetchRates(): FetchedRates = withContext(Dispatchers.IO) {
        coroutineScope {
            val bcv = async { getJson(BCV_URL)?.let { BcvRateParser.parse(it, "oficial", "BCV") } }
            val paralelo = async {
                getJson(PARALELO_URL)?.let { BcvRateParser.parse(it, "paralelo", "Paralelo") }
            }
            val eur = async { getJson(EUR_URL)?.let { EurRateParser.parse(it) } }
            val ars = async { getJson(ARS_URL)?.let { ArsRateParser.parse(it) } }
            val bulk = async {
                val fetchedAt = System.currentTimeMillis()
                getJson(BULK_URL)?.let {
                    BulkRateParser.parse(it, CurrencyCatalog.all.map { info -> info.code }.toSet(), fetchedAt)
                } ?: emptyMap()
            }

            val bcvRate = bcv.await()
            val paraleloRate = paralelo.await()
            val eurRate = eur.await()
            val arsRate = ars.await()
            val bulkRates = bulk.await()

            FetchedRates(
                ves = buildMap {
                    bcvRate?.let { put(VesReference.BCV, it) }
                    paraleloRate?.let {
                        put(VesReference.PARALELO, it)
                        // En Venezuela USDT se mueve pegado al paralelo.
                        put(VesReference.USDT, it.copy(source = "USDT (paralelo)"))
                    }
                },
                currencies = buildMap {
                    // Primero la tabla global; luego las fuentes más específicas.
                    putAll(bulkRates)
                    eurRate?.let { put("EUR", it) }
                    arsRate?.let { put("ARS", it) }
                },
            )
        }
    }

    private fun getJson(url: String): String? {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "${Branding.APP_NAME}/${Branding.VERSION} (offline-first)")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    null
                } else {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            } finally {
                connection.disconnect()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val BCV_URL = "https://ve.dolarapi.com/v1/dolares/oficial"
        const val PARALELO_URL = "https://ve.dolarapi.com/v1/dolares/paralelo"
        const val EUR_URL = "https://api.frankfurter.app/latest?from=USD&to=EUR"
        const val ARS_URL = "https://dolarapi.com/v1/dolares/oficial"
        const val BULK_URL = "https://open.er-api.com/v6/latest/USD"
        const val TIMEOUT_MS = 8_000
    }
}
