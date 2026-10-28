package com.suarezmillan.finanzas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.suarezmillan.finanzas.core.settings.ThemeMode
import com.suarezmillan.finanzas.core.settings.ThemePreset

/**
 * Acentos por tema. Las superficies (grafito/nieve), la semántica de flujo y
 * los colores de divisa quedan fijos: el tema es la marca, no el significado.
 */
private data class AccentPalette(
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
)

private fun paletteFor(preset: ThemePreset): AccentPalette = when (preset) {
    ThemePreset.GRAFITO -> AccentPalette(
        darkPrimary = DivisaLocal,
        darkOnPrimary = Color(0xFF04181F),
        darkContainer = Color(0xFF123B46),
        darkOnContainer = DivisaLocal,
        lightPrimary = DivisaLocalOscura,
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFD6F1F8),
        lightOnContainer = Color(0xFF06323D),
    )

    ThemePreset.OCEANO -> AccentPalette(
        darkPrimary = Color(0xFF6FB4FF),
        darkOnPrimary = Color(0xFF04182B),
        darkContainer = Color(0xFF16324F),
        darkOnContainer = Color(0xFFA9CDFF),
        lightPrimary = Color(0xFF155FA8),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFD9E8FF),
        lightOnContainer = Color(0xFF0A2E4F),
    )

    ThemePreset.BOSQUE -> AccentPalette(
        darkPrimary = Color(0xFF5FD6A6),
        darkOnPrimary = Color(0xFF042115),
        darkContainer = Color(0xFF143A2C),
        darkOnContainer = Color(0xFF9BE8C7),
        lightPrimary = Color(0xFF17795A),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFD4F3E6),
        lightOnContainer = Color(0xFF073024),
    )

    ThemePreset.ATARDECER -> AccentPalette(
        darkPrimary = Color(0xFFFFA477),
        darkOnPrimary = Color(0xFF2B0F03),
        darkContainer = Color(0xFF40251A),
        darkOnContainer = Color(0xFFFFC9AF),
        lightPrimary = Color(0xFFB4461A),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFFFE2D3),
        lightOnContainer = Color(0xFF4A1D05),
    )

    ThemePreset.LAVANDA -> AccentPalette(
        darkPrimary = Color(0xFFC0A6FF),
        darkOnPrimary = Color(0xFF1B0B3A),
        darkContainer = Color(0xFF2C2244),
        darkOnContainer = Color(0xFFDCCBFF),
        lightPrimary = Color(0xFF6D4AC7),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFEAE1FF),
        lightOnContainer = Color(0xFF2A1552),
    )

    ThemePreset.MONO -> AccentPalette(
        darkPrimary = Color(0xFFD3DCE6),
        darkOnPrimary = Color(0xFF12171D),
        darkContainer = Color(0xFF2A333D),
        darkOnContainer = Color(0xFFE3E9F0),
        lightPrimary = Color(0xFF34414E),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFE6EBF1),
        lightOnContainer = Color(0xFF141A21),
    )
}

private fun schemeFor(preset: ThemePreset, dark: Boolean): ColorScheme {
    val accent = paletteFor(preset)
    return if (dark) {
        FinanzasDark.copy(
            primary = accent.darkPrimary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkContainer,
            onPrimaryContainer = accent.darkOnContainer,
        )
    } else {
        FinanzasLight.copy(
            primary = accent.lightPrimary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightContainer,
            onPrimaryContainer = accent.lightOnContainer,
        )
    }
}

/** Color de muestra para el selector de temas. */
fun presetSwatch(preset: ThemePreset, dark: Boolean = true): Color {
    val accent = paletteFor(preset)
    return if (dark) accent.darkPrimary else accent.lightPrimary
}

/**
 * Tema de la app.
 *
 * Nota de migración: con `androidx.compose.material3:material3:1.4.0+` se puede
 * sustituir `MaterialTheme` por `MaterialExpressiveTheme` para las formas y
 * transiciones de M3 Expressive.
 */
@Composable
fun FinanzasTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    preset: ThemePreset = ThemePreset.GRAFITO,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = schemeFor(preset, dark),
        typography = FinTypography,
        shapes = FinShapes,
        content = content,
    )
}
