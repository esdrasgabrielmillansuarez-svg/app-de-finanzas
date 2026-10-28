package com.suarezmillan.finanzas.data.local.model

/** Proyección de sobre: límite vs. gastado del mes. */
data class EnvelopeProgress(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val currency: String,
    val limitMinor: Long,
    val spentMinor: Long,
) {
    val remainingMinor: Long get() = limitMinor - spentMinor
    val usedRatio: Float
        get() = if (limitMinor <= 0L) 0f else (spentMinor.toFloat() / limitMinor).coerceIn(0f, 2f)
    val isOver: Boolean get() = spentMinor > limitMinor
    val isWarning: Boolean get() = !isOver && usedRatio >= 0.8f
}
