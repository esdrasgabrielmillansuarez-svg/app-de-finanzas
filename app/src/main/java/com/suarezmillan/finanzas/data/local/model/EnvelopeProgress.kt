package com.suarezmillan.finanzas.data.local.model

import androidx.compose.runtime.Immutable
import com.suarezmillan.finanzas.core.budget.EnvelopePeriod

/**
 * Progreso de un sobre en su periodo vigente.
 *
 * [limitMinor] ya incluye [carryMinor]; [baseLimitMinor] es el límite que el
 * usuario escribió. El gasto se calcula solo con movimientos del periodo.
 */
@Immutable
data class EnvelopeProgress(
    val categoryId: String,
    val categoryName: String,
    val name: String,
    val emoji: String,
    val currency: String,
    val period: EnvelopePeriod,
    val rollover: Boolean,
    val baseLimitMinor: Long,
    val carryMinor: Long,
    val limitMinor: Long,
    val spentMinor: Long,
    val windowLabel: String,
    val windowStartMillis: Long,
    val windowEndMillis: Long,
) {
    val remainingMinor: Long get() = limitMinor - spentMinor

    val usedRatio: Float
        get() = if (limitMinor <= 0L) 0f else (spentMinor.toFloat() / limitMinor).coerceIn(0f, 2f)

    val isOver: Boolean get() = spentMinor > limitMinor

    val isWarning: Boolean get() = !isOver && usedRatio >= 0.8f
}

/** Datos que envía el editor de sobres; el repositorio conserva `id`/`createdAt`. */
@Immutable
data class EnvelopeInput(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val limitMinor: Long,
    val currency: String,
    val period: EnvelopePeriod,
    val rollover: Boolean,
)
