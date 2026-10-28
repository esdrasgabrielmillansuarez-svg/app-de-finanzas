package com.suarezmillan.finanzas.di

import android.content.Context
import com.suarezmillan.finanzas.core.settings.PreferenceStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** Preferencias reales del teléfono (sin DataStore todavía). */
class SharedPreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) : PreferenceStore {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun getBoolean(key: String, fallback: Boolean): Boolean =
        prefs.getBoolean(key, fallback)

    override fun getLong(key: String, fallback: Long): Long = prefs.getLong(key, fallback)

    // Se guarda como texto: SharedPreferences solo tiene float y perdería
    // decimales de una tasa como 888,1183.
    override fun getDouble(key: String, fallback: Double): Double =
        prefs.getString(key, null)?.toDoubleOrNull() ?: fallback

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun putDouble(key: String, value: Double) {
        prefs.edit().putString(key, value.toString()).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private companion object {
        const val PREFS = "finanzas_settings"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindPreferenceStore(impl: SharedPreferenceStore): PreferenceStore
}
