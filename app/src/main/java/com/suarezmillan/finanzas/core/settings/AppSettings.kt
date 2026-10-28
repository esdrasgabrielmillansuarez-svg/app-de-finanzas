package com.suarezmillan.finanzas.core.settings

import com.suarezmillan.finanzas.core.currency.CurrencyCatalog
import com.suarezmillan.finanzas.core.currency.FetchedRates
import com.suarezmillan.finanzas.core.currency.LiveRate
import com.suarezmillan.finanzas.core.currency.VesReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Preferencias de la app.
 *
 * La apariencia, los recordatorios y las tasas se persisten vía
 * [PreferenceStore] (lectura síncrona al arrancar). El resto del estado de
 * divisas sigue en memoria.
 *
 * Prioridad de una tasa: manual del usuario > referencia en vivo > tabla
 * offline de [CurrencyCatalog].
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
     * Tasas manuales de divisas que no sean VES: código → unidades por 1 USD.
     * Para el bolívar hay una tasa manual por cada referencia ([vesManualRates]).
     */
    private val _manualUnitsPerUsd = MutableStateFlow<Map<String, Double>>(loadManualRates())
    val manualUnitsPerUsd: StateFlow<Map<String, Double>> = _manualUnitsPerUsd.asStateFlow()

    /** Última cotización de cada referencia del bolívar. */
    private val _vesReferences = MutableStateFlow(loadVesReferences())
    val vesReferences: StateFlow<Map<VesReference, LiveRate>> = _vesReferences.asStateFlow()

    /** Referencia del bolívar que usa la app para convertir. */
    private val _activeVesReference = MutableStateFlow(
        VesReference.fromId(store.getString(KEY_ACTIVE_VES_REF)),
    )
    val activeVesReference: StateFlow<VesReference> = _activeVesReference.asStateFlow()

    /** Overrides manuales de VES, por referencia. */
    private val _vesManualRates = MutableStateFlow(loadVesManualRates())
    val vesManualRates: StateFlow<Map<VesReference, Double>> = _vesManualRates.asStateFlow()

    /** Cotizaciones en vivo de otras divisas (hoy EUR y ARS): código → tasa. */
    private val _liveRates = MutableStateFlow(loadLiveRates())
    val liveRates: StateFlow<Map<String, LiveRate>> = _liveRates.asStateFlow()

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

    /**
     * Emite cada vez que cambia cualquier tasa. Sirve para recalcular las
     * cotizaciones sin exponer cinco flujos por todos lados.
     */
    val ratesRevision: Flow<Unit> = combine(
        _manualUnitsPerUsd,
        _vesReferences,
        _activeVesReference,
        _vesManualRates,
        _liveRates,
    ) { _, _, _, _, _ -> Unit }

    // ------------------------------------------------------------------ divisas

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
        if (!isValidRate(unitsPerUsd)) return
        val normalized = code.trim().uppercase()
        if (normalized == VES_CODE) {
            setVesManualRate(_activeVesReference.value, unitsPerUsd)
            return
        }
        _manualUnitsPerUsd.value = _manualUnitsPerUsd.value + (normalized to unitsPerUsd)
        store.putDouble(manualKey(normalized), unitsPerUsd)
    }

    fun clearManualRate(code: String) {
        val normalized = code.trim().uppercase()
        if (normalized == VES_CODE) {
            clearVesManualRate(_activeVesReference.value)
            return
        }
        if (!_manualUnitsPerUsd.value.containsKey(normalized)) return
        _manualUnitsPerUsd.value = _manualUnitsPerUsd.value - normalized
        store.remove(manualKey(normalized))
    }

    fun isManual(code: String): Boolean {
        val normalized = code.trim().uppercase()
        return if (normalized == VES_CODE) {
            _vesManualRates.value.containsKey(_activeVesReference.value)
        } else {
            _manualUnitsPerUsd.value.containsKey(normalized)
        }
    }

    // ------------------------------------------------------------------ referencias VES

    fun setActiveVesReference(reference: VesReference) {
        if (_activeVesReference.value == reference) return
        _activeVesReference.value = reference
        store.putString(KEY_ACTIVE_VES_REF, reference.id)
    }

    fun setVesManualRate(reference: VesReference, unitsPerUsd: Double) {
        if (!isValidRate(unitsPerUsd)) return
        _vesManualRates.value = _vesManualRates.value + (reference to unitsPerUsd)
        store.putDouble(vesManualKey(reference), unitsPerUsd)
    }

    fun clearVesManualRate(reference: VesReference) {
        if (!_vesManualRates.value.containsKey(reference)) return
        _vesManualRates.value = _vesManualRates.value - reference
        store.remove(vesManualKey(reference))
    }

    fun setVesReferences(rates: Map<VesReference, LiveRate>) {
        val valid = rates.filterValues { it.isFresh }
        if (valid.isEmpty()) return
        _vesReferences.value = _vesReferences.value + valid
        valid.forEach { (reference, rate) ->
            store.putDouble(vesValueKey(reference), rate.unitsPerUsd)
            store.putLong(vesAtKey(reference), rate.fetchedAtMillis)
            store.putString(vesSourceKey(reference), rate.source)
        }
    }

    fun setLiveRates(rates: Map<String, LiveRate>) {
        val valid = rates
            .mapKeys { (code, _) -> code.trim().uppercase() }
            .filterValues { it.isFresh }
        if (valid.isEmpty()) return
        _liveRates.value = _liveRates.value + valid
        valid.forEach { (code, rate) ->
            store.putDouble(liveValueKey(code), rate.unitsPerUsd)
            store.putLong(liveAtKey(code), rate.fetchedAtMillis)
            store.putString(liveSourceKey(code), rate.source)
        }
    }

    fun updateRates(fetched: FetchedRates) {
        setVesReferences(fetched.ves)
        setLiveRates(fetched.currencies)
    }

    fun activeVesRate(): LiveRate? =
        _vesReferences.value[_activeVesReference.value]?.takeIf { it.isFresh }

    fun liveRateFor(code: String): LiveRate? {
        val normalized = code.trim().uppercase()
        return if (normalized == VES_CODE) {
            activeVesRate()
        } else {
            _liveRates.value[normalized]?.takeIf { it.isFresh }
        }
    }

    /** Todas las referencias vigentes, indexadas por código (VES usa la activa). */
    fun allLiveRates(): Map<String, LiveRate> = buildMap {
        activeVesRate()?.let { put(VES_CODE, it) }
        _liveRates.value.forEach { (code, rate) ->
            if (rate.isFresh) put(code, rate)
        }
    }

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

    // ------------------------------------------------------------------ carga

    private fun loadVesReferences(): Map<VesReference, LiveRate> = buildMap {
        VesReference.entries.forEach { reference ->
            readRate(
                valueKey = vesValueKey(reference),
                atKey = vesAtKey(reference),
                sourceKey = vesSourceKey(reference),
                fallbackSource = reference.label,
            )?.let { put(reference, it) }
        }
        // Migración de la versión anterior, que solo guardaba BCV.
        if (!containsKey(VesReference.BCV)) {
            readRate(
                valueKey = KEY_LEGACY_VES_VALUE,
                atKey = KEY_LEGACY_VES_AT,
                sourceKey = KEY_LEGACY_VES_SOURCE,
                fallbackSource = "BCV",
            )?.let { put(VesReference.BCV, it) }
        }
    }

    private fun loadVesManualRates(): Map<VesReference, Double> = buildMap {
        VesReference.entries.forEach { reference ->
            val value = store.getDouble(vesManualKey(reference), 0.0)
            if (isValidRate(value)) put(reference, value)
        }
    }

    private fun loadLiveRates(): Map<String, LiveRate> = buildMap {
        LIVE_CURRENCY_CODES.forEach { code ->
            readRate(
                valueKey = liveValueKey(code),
                atKey = liveAtKey(code),
                sourceKey = liveSourceKey(code),
                fallbackSource = code,
            )?.let { put(code, it) }
        }
    }

    private fun loadManualRates(): Map<String, Double> = buildMap {
        CurrencyCatalog.all.forEach { info ->
            if (info.code == VES_CODE) return@forEach
            val value = store.getDouble(manualKey(info.code), 0.0)
            if (isValidRate(value)) put(info.code, value)
        }
    }

    private fun readRate(
        valueKey: String,
        atKey: String,
        sourceKey: String,
        fallbackSource: String,
    ): LiveRate? {
        val value = store.getDouble(valueKey, 0.0)
        if (!isValidRate(value)) return null
        return LiveRate(
            unitsPerUsd = value,
            fetchedAtMillis = store.getLong(atKey, 0L),
            source = store.getString(sourceKey) ?: fallbackSource,
        )
    }

    private fun isValidRate(value: Double): Boolean =
        value > 0.0 && !value.isNaN() && !value.isInfinite()

    private fun vesValueKey(reference: VesReference) = "ves_ref_${reference.id}_value"
    private fun vesAtKey(reference: VesReference) = "ves_ref_${reference.id}_at"
    private fun vesSourceKey(reference: VesReference) = "ves_ref_${reference.id}_source"
    private fun vesManualKey(reference: VesReference) = "ves_manual_${reference.id}"

    private fun liveValueKey(code: String) = "live_${code}_value"
    private fun liveAtKey(code: String) = "live_${code}_at"
    private fun liveSourceKey(code: String) = "live_${code}_source"
    private fun manualKey(code: String) = "manual_${code}_units_per_usd"

    private companion object {
        const val VES_CODE = "VES"
        val LIVE_CURRENCY_CODES = listOf("EUR", "ARS")

        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_PRESET = "theme_preset"
        const val KEY_REMINDERS = "reminders_enabled"
        const val KEY_ACTIVE_VES_REF = "active_ves_reference"

        // Claves de la versión 0.3.0: solo había BCV.
        const val KEY_LEGACY_VES_VALUE = "live_ves_units_per_usd"
        const val KEY_LEGACY_VES_AT = "live_ves_fetched_at"
        const val KEY_LEGACY_VES_SOURCE = "live_ves_source"
    }
}
