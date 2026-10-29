package com.suarezmillan.finanzas.data.local.model

import androidx.compose.runtime.Immutable
import com.suarezmillan.finanzas.core.budget.EnvelopePeriod

/**
 * Datos base de un sobre, ya unidos con su categoría.
 *
 * `categoryName` / `categoryEmoji` identifican qué gastos entran; `name` /
 * `emoji` son la identidad propia que eligió el usuario.
 */
@Immutable
data class EnvelopeDefinition(
    val categoryId: String,
    val categoryName: String,
    val categoryEmoji: String,
    val name: String,
    val emoji: String,
    val currency: String,
    val limitMinor: Long,
    val period: EnvelopePeriod,
    val rollover: Boolean,
    val createdAt: Long,
)
