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
    fun getLong(key: String, fallback: Long): Long
    fun getDouble(key: String, fallback: Double): Double
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun putLong(key: String, value: Long)
    fun putDouble(key: String, value: Double)
    fun remove(key: String)
}

/** Implementación volátil: tests y previews. */
class InMemoryPreferenceStore(
    initial: Map<String, String> = emptyMap(),
    initialBooleans: Map<String, Boolean> = emptyMap(),
) : PreferenceStore {

    private val values = initial.toMutableMap()
    private val booleans = initialBooleans.toMutableMap()
    private val longs = mutableMapOf<String, Long>()
    private val doubles = mutableMapOf<String, Double>()

    override fun getString(key: String): String? = values[key]

    override fun getBoolean(key: String, fallback: Boolean): Boolean = booleans[key] ?: fallback

    override fun getLong(key: String, fallback: Long): Long = longs[key] ?: fallback

    override fun getDouble(key: String, fallback: Double): Double = doubles[key] ?: fallback

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }

    override fun putLong(key: String, value: Long) {
        longs[key] = value
    }

    override fun putDouble(key: String, value: Double) {
        doubles[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
        booleans.remove(key)
        longs.remove(key)
        doubles.remove(key)
    }
}
