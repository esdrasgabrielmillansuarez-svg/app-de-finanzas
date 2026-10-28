package com.suarezmillan.finanzas.core.currency.remote

import com.suarezmillan.finanzas.core.currency.LiveRate
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fuente de cotizaciones externas. Interfaz para poder sustituirla en tests. */
interface RateSource {
    suspend fun fetchBcvRate(): LiveRate?
}

/**
 * Cotización oficial del Banco Central de Venezuela.
 *
 * `bcv.org.ve` no es accesible de forma estable desde Android (TLS y bloqueos),
 * así que se consulta el espejo público `ve.dolarapi.com`, que identifica el
 * dato como `fuente: "oficial"` y lo actualiza con el BCV. La app no envía
 * ningún dato: es un GET anónimo.
 *
 * TODO si algún día hay un endpoint estable del BCV: cambiar solo esta clase.
 */
class BcvRateSource @Inject constructor() : RateSource {

    override suspend fun fetchBcvRate(): LiveRate? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Finanzas/1.0 (offline-first)")
            }
            try {
                if (connection.responseCode !in 200..299) return@withContext null
                val body = connection.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                BcvRateParser.parse(body)
            } finally {
                connection.disconnect()
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val ENDPOINT = "https://ve.dolarapi.com/v1/dolares/oficial"
        const val TIMEOUT_MS = 8_000
    }
}
