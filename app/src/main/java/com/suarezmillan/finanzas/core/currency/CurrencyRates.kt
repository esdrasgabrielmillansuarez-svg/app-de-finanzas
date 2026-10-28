package com.suarezmillan.finanzas.core.currency

import com.suarezmillan.finanzas.core.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.roundToLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Una divisa ya resuelta contra USD y EUR, lista para pintar.
 *
 * - [usdPerUnit]: cuántos USD vale 1 unidad de esta divisa (`0.000952` para ARS).
 * - [eurPerUnit]: ídem en euros, derivado de la referencia EUR/USD.
 */
data class CurrencyQuote(
    val code: String,
    val name: String,
    val symbol: String,
    val region: Region,
    val countries: List<Country>,
    val unitsPerUsd: Double,
    val usdPerUnit: Double,
    val eurPerUnit: Double,
    val isManual: Boolean,
    /** true si el valor viene de una cotización externa vigente (BCV). */
    val isLive: Boolean = false,
    val liveUpdatedAtMillis: Long? = null,
)

/**
 * Conversor único del producto.
 *
 * Todo pasa por USD como pivote: `A → B = (unidades B por USD) / (unidades A por USD)`.
 * Así hay **una sola fuente de verdad** y no se acumulan tasas cruzadas
 * inconsistentes entre pantallas.
 */
@Singleton
class CurrencyRates @Inject constructor(
    private val settings: AppSettings,
) {

    fun unitsPerUsd(code: String): Double {
        val normalized = code.trim().uppercase()
        // Prioridad: tasa manual del usuario > cotización externa vigente > referencia offline.
        settings.manualUnitsPerUsd.value[normalized]?.let { return it }
        if (normalized == VES) {
            settings.liveRate.value?.takeIf { it.isFresh }?.let { return it.unitsPerUsd }
        }
        return CurrencyCatalog.unitsPerUsd(normalized)
    }

    fun usdPerUnit(code: String): Double = 1.0 / unitsPerUsd(code)

    fun eurPerUnit(code: String): Double = usdPerUnit(code) * unitsPerUsd(EUR)

    /** Cuántas unidades de [to] compran 1 unidad de [from]. */
    fun rate(from: String, to: String): Double {
        val fromUnits = unitsPerUsd(from)
        val toUnits = unitsPerUsd(to)
        if (fromUnits <= 0.0 || toUnits <= 0.0) return 1.0
        return toUnits / fromUnits
    }

    fun convert(minor: Long, from: String, to: String): Long =
        (minor * rate(from, to)).roundToLong()

    fun quote(code: String): CurrencyQuote {
        val info = CurrencyCatalog.find(code) ?: unknownCurrency(code)
        val manual = settings.isManual(info.code)
        val live = settings.liveRate.value
            ?.takeIf { it.isFresh && info.code == VES && !manual }
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
            liveUpdatedAtMillis = live?.fetchedAtMillis,
        )
    }

    fun quotes(): List<CurrencyQuote> = CurrencyCatalog.all.map { quote(it.code) }

    /** Se recalcula cuando el usuario fija una tasa manual o llega la cotización BCV. */
    fun quotesFlow(): Flow<List<CurrencyQuote>> = combine(
        settings.manualUnitsPerUsd,
        settings.liveRate,
    ) { _, _ -> quotes() }

    private fun unknownCurrency(code: String): CurrencyInfo = CurrencyInfo(
        code = code.trim().uppercase(),
        name = code.trim().uppercase(),
        symbol = code.trim().uppercase(),
        region = Region.AMERICA,
        unitsPerUsd = 1.0,
        countries = emptyList(),
    )

    companion object {
        const val USD = "USD"
        const val EUR = "EUR"
        const val VES = "VES"
    }
}
