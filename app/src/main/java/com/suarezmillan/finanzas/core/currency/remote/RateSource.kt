package com.suarezmillan.finanzas.core.currency.remote

import com.suarezmillan.finanzas.core.currency.FetchedRates

/** Fuente de cotizaciones externas. Interfaz para poder sustituirla en tests. */
interface RateSource {
    suspend fun fetchRates(): FetchedRates
}
