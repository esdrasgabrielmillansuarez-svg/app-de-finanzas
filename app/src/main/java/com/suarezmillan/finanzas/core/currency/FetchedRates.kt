package com.suarezmillan.finanzas.core.currency

/**
 * Resultado de una actualización de referencias.
 *
 * Puede venir parcial: si el BCV responde pero el euro no, se actualiza lo que
 * llegó y se conserva lo anterior para el resto.
 */
data class FetchedRates(
    val ves: Map<VesReference, LiveRate> = emptyMap(),
    /** Otras divisas: EUR, ARS y la tabla global del catálogo. */
    val currencies: Map<String, LiveRate> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = ves.isEmpty() && currencies.isEmpty()
}
