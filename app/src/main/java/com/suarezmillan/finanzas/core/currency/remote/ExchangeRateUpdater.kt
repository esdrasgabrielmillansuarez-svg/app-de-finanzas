package com.suarezmillan.finanzas.core.currency.remote

import com.suarezmillan.finanzas.core.currency.FetchedRates
import com.suarezmillan.finanzas.core.settings.AppSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado visible de la última actualización de referencias. */
data class RateRefreshState(
    val loading: Boolean = false,
    val error: String? = null,
    val lastAttemptMillis: Long? = null,
)

/**
 * Actualiza y cachea las referencias de cambio.
 *
 * - No bloquea la UI: todo es suspendido y con timeout.
 * - Si las referencias tienen menos de 6 horas, no vuelve a pedir salvo que el
 *   usuario toque "Actualizar".
 * - Si falla parte de la descarga, conserva lo anterior; si falla todo, expone
 *   un error corto y honesto.
 */
@Singleton
class ExchangeRateUpdater @Inject constructor(
    private val source: RateSource,
    private val settings: AppSettings,
) {

    private val _state = MutableStateFlow(RateRefreshState())
    val state: StateFlow<RateRefreshState> = _state.asStateFlow()

    /** Devuelve true si trajo al menos una referencia nueva. */
    suspend fun refresh(force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        val cached = settings.allLiveRates()

        // La tabla global puede llenar la caché con decenas de divisas sin que
        // BCV, EUR o ARS estén presentes. Solo se evita la descarga cuando las
        // referencias específicas están frescas.
        if (!force && REQUIRED_CODES.all { code ->
                cached[code]?.let { isFreshEnough(it.fetchedAtMillis, now) } == true
            }
        ) {
            return false
        }
        if (_state.value.loading) return false

        _state.value = RateRefreshState(loading = true, lastAttemptMillis = now)

        val fetched = try {
            source.fetchRates()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            FetchedRates()
        }

        return if (!fetched.isEmpty) {
            settings.updateRates(fetched)
            _state.value = RateRefreshState(loading = false, lastAttemptMillis = now)
            true
        } else {
            _state.value = RateRefreshState(
                loading = false,
                error = "No se pudo actualizar ninguna referencia. Se mantienen las últimas.",
                lastAttemptMillis = now,
            )
            false
        }
    }

    companion object {
        /** Seis horas: suficiente para el día sin castigar la batería ni los APIs. */
        const val MAX_AGE_MILLIS = 6L * 60L * 60L * 1000L

        /** Referencias específicas que deben existir (VES activa, EUR y ARS). */
        private val REQUIRED_CODES = listOf("VES", "EUR", "ARS")

        fun isFreshEnough(fetchedAtMillis: Long, nowMillis: Long): Boolean {
            val age = nowMillis - fetchedAtMillis
            return age in 0 until MAX_AGE_MILLIS
        }
    }
}
