package com.suarezmillan.finanzas.core.settings

/**
 * Almacenamiento clave-valor mínimo para las preferencias.
 *
 * Existe para que [AppSettings] siga siendo testeable en la JVM (sin Android).
 * La implementación real usa SharedPreferences; los tests usan
 * [InMemoryPreferenceStore].
 */
interface PreferenceStore {
    fun getString(key: String): String?
    fun getBoolean(key: String, fallback: Boolean): Boolean
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
}

/** Implementación volátil: tests y previews. */
class InMemoryPreferenceStore(
    initial: Map<String, String> = emptyMap(),
    initialBooleans: Map<String, Boolean> = emptyMap(),
) : PreferenceStore {

    private val values = initial.toMutableMap()
    private val booleans = initialBooleans.toMutableMap()

    override fun getString(key: String): String? = values[key]

    override fun getBoolean(key: String, fallback: Boolean): Boolean = booleans[key] ?: fallback

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }
}
