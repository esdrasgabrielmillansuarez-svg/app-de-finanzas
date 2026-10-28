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
 * La apariencia y los recordatorios se persisten ya vía [PreferenceStore]
 * (lectura síncrona al arrancar). Divisas y tasas siguen en memoria.
 *
 * TODO(Sprint 4): mover todo a DataStore; la forma de estas APIs no cambia.
 */
@Singleton
class AppSettings @Inject constructor(
    private val store: PreferenceStore,
) {

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

    private val _themeMode = MutableStateFlow(
        ThemeMode.fromId(store.getString(KEY_THEME_MODE)),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _themePreset = MutableStateFlow(
        ThemePreset.fromId(store.getString(KEY_THEME_PRESET)),
    )
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(
        store.getBoolean(KEY_REMINDERS, true),
    )
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

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

    // ------------------------------------------------------------------ apariencia

    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value == mode) return
        _themeMode.value = mode
        store.putString(KEY_THEME_MODE, mode.id)
    }

    fun setThemePreset(preset: ThemePreset) {
        if (_themePreset.value == preset) return
        _themePreset.value = preset
        store.putString(KEY_THEME_PRESET, preset.id)
    }

    // ------------------------------------------------------------------ recordatorios

    fun setRemindersEnabled(enabled: Boolean) {
        if (_remindersEnabled.value == enabled) return
        _remindersEnabled.value = enabled
        store.putBoolean(KEY_REMINDERS, enabled)
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_PRESET = "theme_preset"
        const val KEY_REMINDERS = "reminders_enabled"
    }
}
