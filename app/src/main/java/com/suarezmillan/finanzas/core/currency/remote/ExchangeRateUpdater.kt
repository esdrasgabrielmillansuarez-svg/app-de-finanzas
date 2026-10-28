package com.suarezmillan.finanzas.core.currency.remote

import com.suarezmillan.finanzas.core.settings.AppSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado visible de la última actualización de la cotización BCV. */
data class RateRefreshState(
    val loading: Boolean = false,
    val error: String? = null,
    val lastAttemptMillis: Long? = null,
)

/**
 * Actualiza y cachea la cotización oficial del BCV.
 *
 * - No bloquea la UI: todo es suspendido y con timeout.
 * - Si ya hay una cotización de las últimas 6 horas, no vuelve a pedir salvo
 *   que el usuario toque "Actualizar".
 * - Si falla (sin red, sitio caído), conserva la última cotización conocida y
 *   expone un error corto para que la UI sea honesta.
 */
@Singleton
class ExchangeRateUpdater @Inject constructor(
    private val source: RateSource,
    private val settings: AppSettings,
) {

    private val _state = MutableStateFlow(RateRefreshState())
    val state: StateFlow<RateRefreshState> = _state.asStateFlow()

    /** Devuelve true si trajo una cotización nueva. */
    suspend fun refresh(force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        val cached = settings.liveRate.value

        if (!force && cached != null && isFreshEnough(cached.fetchedAtMillis, now)) {
            return false
        }
        if (_state.value.loading) return false

        _state.value = RateRefreshState(loading = true, lastAttemptMillis = now)

        val fetched = try {
            source.fetchBcvRate()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        return if (fetched != null) {
            settings.setLiveRate(fetched)
            _state.value = RateRefreshState(loading = false, lastAttemptMillis = now)
            true
        } else {
            _state.value = RateRefreshState(
                loading = false,
                error = "No se pudo conectar con el BCV. Se mantiene la última cotización.",
                lastAttemptMillis = now,
            )
            false
        }
    }

    companion object {
        /** Seis horas: suficiente para el día sin castigar la batería ni el API. */
        const val MAX_AGE_MILLIS = 6L * 60L * 60L * 1000L

        fun isFreshEnough(fetchedAtMillis: Long, nowMillis: Long): Boolean {
            val age = nowMillis - fetchedAtMillis
            return age in 0 until MAX_AGE_MILLIS
        }
    }
}
