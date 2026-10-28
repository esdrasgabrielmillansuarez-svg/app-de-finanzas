package com.suarezmillan.finanzas.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Movimiento: springs cortos, sin rebote exagerado. Un solo gesto "wow" en el
 * producto (el count-up del saldo); todo lo demás obedece al sistema.
 */
object Motion {
    val standard = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 400f,
    )

    val gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}
