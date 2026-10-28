package com.suarezmillan.finanzas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema de la app.
 *
 * Nota de migración: con `androidx.compose.material3:material3:1.4.0+` se puede
 * sustituir `MaterialTheme` por `MaterialExpressiveTheme` (mismos parámetros +
 * `motionScheme = MotionScheme.expressive()`) para obtener las formas y
 * transiciones de M3 Expressive. Se deja en `MaterialTheme` porque la versión
 * fijada en el catálogo (BOM 2024.10.01 → material3 1.3.x) no lo incluye.
 */
@Composable
fun FinanzasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) FinanzasDark else FinanzasLight,
        typography = FinTypography,
        shapes = FinShapes,
        content = content,
    )
}
