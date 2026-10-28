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

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
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
