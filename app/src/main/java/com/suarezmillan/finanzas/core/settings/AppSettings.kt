package com.suarezmillan.finanzas.core.settings

import com.suarezmillan.finanzas.core.currency.CurrencyCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferencias de la app.
 *
 * TODO(Fase 1, Sprint 3): mover a DataStore. La forma de estas APIs no cambia,
 * solo el almacenamiento.
 */
@Singleton
class AppSettings @Inject constructor() {

    private val _baseCurrency = MutableStateFlow("ARS")
    val baseCurrency: StateFlow<String> = _baseCurrency.asStateFlow()

    private val _secondaryCurrency = MutableStateFlow("USD")
    val secondaryCurrency: StateFlow<String> = _secondaryCurrency.asStateFlow()

    /**
     * Tasas fijadas a mano por el usuario: código → unidades por 1 USD.
     * Vacío = se usa la referencia offline de [CurrencyCatalog].
     */
    private val _manualUnitsPerUsd = MutableStateFlow<Map<String, Double>>(emptyMap())
    val manualUnitsPerUsd: StateFlow<Map<String, Double>> = _manualUnitsPerUsd.asStateFlow()

    fun setBaseCurrency(code: String) {
        val normalized = code.trim().uppercase()
        if (CurrencyCatalog.find(normalized) == null) return
        if (normalized == _secondaryCurrency.value) return
        _baseCurrency.value = normalized
    }

    fun setSecondaryCurrency(code: String) {
        val normalized = code.trim().uppercase()
        if (CurrencyCatalog.find(normalized) == null) return
        if (normalized == _baseCurrency.value) return
        _secondaryCurrency.value = normalized
    }

    /** Fija la cotización manual de [code]: cuántas unidades equivalen a 1 USD. */
    fun setManualRate(code: String, unitsPerUsd: Double) {
        if (unitsPerUsd <= 0.0 || unitsPerUsd.isNaN() || unitsPerUsd.isInfinite()) return
        _manualUnitsPerUsd.value = _manualUnitsPerUsd.value + (code.trim().uppercase() to unitsPerUsd)
    }

    fun clearManualRate(code: String) {
        val normalized = code.trim().uppercase()
        if (!_manualUnitsPerUsd.value.containsKey(normalized)) return
        _manualUnitsPerUsd.value = _manualUnitsPerUsd.value - normalized
    }

    fun isManual(code: String): Boolean =
        _manualUnitsPerUsd.value.containsKey(code.trim().uppercase())
}
