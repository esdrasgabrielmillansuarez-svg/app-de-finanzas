package com.suarezmillan.finanzas.data.local.model

/** Datos base de un sobre, sin el gasto calculado. */
data class EnvelopeDefinition(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val currency: String,
    val limitMinor: Long,
)
