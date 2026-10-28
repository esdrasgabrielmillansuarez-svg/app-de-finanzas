package com.suarezmillan.finanzas.core.currency

import com.suarezmillan.finanzas.core.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Una divisa ya resuelta contra USD y EUR, lista para pintar.
 *
 * Los tres valores son `null` cuando no hay ninguna referencia real ni manual:
 * la UI muestra "sin cotización" en vez de inventar un número.
 */
data class CurrencyQuote(
    val code: String,
    val name: String,
    val symbol: String,
    val region: Region,
    val countries: List<Country>,
    val unitsPerUsd: Double?,
    val usdPerUnit: Double?,
    val eurPerUnit: Double?,
    val isManual: Boolean,
    /** true si el valor viene de una cotización externa vigente. */
    val isLive: Boolean = false,
    /** Etiqueta de la fuente en vivo (BCV, Paralelo, BCE/Frankfurter...). */
    val liveSource: String? = null,
    val liveUpdatedAtMillis: Long? = null,
)

/**
 * Conversor único del producto.
 *
 * Todo pasa por USD como pivote: `A → B = (unidades B por USD) / (unidades A por USD)`.
 * Así hay **una sola fuente de verdad** y no se acumulan tasas cruzadas
 * inconsistentes entre pantallas.
 *
 * Prioridad: manual del usuario > referencia en vivo/cacheada > sin dato.
 * No hay tabla de tasas aproximadas: si no hay fuente, no se inventa.
 */
@Singleton
class CurrencyRates @Inject constructor(
    private val settings: AppSettings,
) {

    fun unitsPerUsd(code: String): Double? {
        val normalized = code.trim().uppercase()

        // Para VES la tasa manual es por referencia (BCV, paralelo, USDT).
        if (normalized == VES) {
            settings.vesManualRates.value[settings.activeVesReference.value]?.let { return it }
            return settings.activeVesRate()?.unitsPerUsd
        }

        settings.manualUnitsPerUsd.value[normalized]?.let { return it }
        if (normalized == USD || normalized == "USDT") return 1.0
        return settings.liveRates.value[normalized]
            ?.takeIf { it.isFresh }
            ?.unitsPerUsd
    }

    fun usdPerUnit(code: String): Double? =
        unitsPerUsd(code)?.takeIf { it > 0.0 }?.let { 1.0 / it }

    fun eurPerUnit(code: String): Double? {
        val usd = usdPerUnit(code) ?: return null
        val eurUnits = unitsPerUsd(EUR) ?: return null
        return usd * eurUnits
    }

    /** Cuántas unidades de [to] compran 1 unidad de [from]; null si falta una tasa. */
    fun rate(from: String, to: String): Double? {
        if (from.trim().equals(to.trim(), ignoreCase = true)) return 1.0
        val fromUnits = unitsPerUsd(from) ?: return null
        val toUnits = unitsPerUsd(to) ?: return null
        if (fromUnits <= 0.0 || toUnits <= 0.0) return null
        return toUnits / fromUnits
    }

    /** Convierte solo si ambas divisas tienen tasa; si no, devuelve null. */
    fun convert(minor: Long, from: String, to: String): Long? =
        rate(from, to)?.let { (minor * it).roundToLong() }

    fun quote(code: String): CurrencyQuote {
        val info = CurrencyCatalog.find(code) ?: unknownCurrency(code)
        val manual = settings.isManual(info.code)
        val live = settings.liveRateFor(info.code)?.takeIf { it.isFresh && !manual }
        return CurrencyQuote(
            code = info.code,
            name = info.name,
            symbol = info.symbol,
            region = info.region,
            countries = info.countries,
            unitsPerUsd = unitsPerUsd(info.code),
            usdPerUnit = usdPerUnit(info.code),
            eurPerUnit = eurPerUnit(info.code),
            isManual = manual,
            isLive = live != null,
            liveSource = live?.source,
            liveUpdatedAtMillis = live?.fetchedAtMillis,
        )
    }

    fun quotes(): List<CurrencyQuote> = CurrencyCatalog.all.map { quote(it.code) }

    /** Se recalcula cuando cambia cualquier tasa manual o referencia en vivo. */
    fun quotesFlow(): Flow<List<CurrencyQuote>> = settings.ratesRevision.map { quotes() }

    private fun unknownCurrency(code: String): CurrencyInfo = CurrencyInfo(
        code = code.trim().uppercase(),
        name = code.trim().uppercase(),
        symbol = code.trim().uppercase(),
        region = Region.AMERICA,
        countries = emptyList(),
    )

    companion object {
        const val USD = "USD"
        const val EUR = "EUR"
        const val VES = "VES"
    }
}
