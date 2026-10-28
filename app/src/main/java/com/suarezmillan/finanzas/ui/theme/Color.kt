package com.suarezmillan.finanzas.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Identidad: "Grafito + dos tintas".
 * El color codifica DIVISA (identidad) y DIRECCIÓN del flujo (semántica).
 * Nunca se usa color como única fuente de información.
 */

// Superficies — dark
val Grafito = Color(0xFF0E1116)
val Superficie = Color(0xFF161B22)
val SuperficieAlta = Color(0xFF1E252E)
val Linea = Color(0xFF252E39)
val TextoPrimario = Color(0xFFF2F5F8)
val TextoSecundario = Color(0xFF93A0AE)
val TextoTerciario = Color(0xFF6B7785)

// Superficies — light
val Nieve = Color(0xFFF7F8FA)
val Blanco = Color(0xFFFFFFFF)
val SuperficieAltaClara = Color(0xFFEEF1F5)
val LineaClara = Color(0xFFE2E7ED)
val TextoPrimarioClaro = Color(0xFF0E1116)
val TextoSecundarioClaro = Color(0xFF5B6673)

// Divisas
val DivisaLocal = Color(0xFF4CC9F0)
val DivisaLocalOscura = Color(0xFF0E86A8)
val DivisaUsd = Color(0xFFFFB020)
val DivisaUsdOscura = Color(0xFFA86A00)

// Semántica de flujo (solo en signos e importes)
val Ingreso = Color(0xFF34D399)
val IngresoClaro = Color(0xFF0B7A55)
val Gasto = Color(0xFFFF7A6B)
val GastoClaro = Color(0xFFB23A2C)

// Estados
val Alerta = Color(0xFFFFB020)
val Peligro = Color(0xFFFF5A5A)

val FinanzasDark = darkColorScheme(
    primary = DivisaLocal,
    onPrimary = Color(0xFF04181F),
    primaryContainer = Color(0xFF123B46),
    onPrimaryContainer = DivisaLocal,
    secondary = DivisaUsd,
    onSecondary = Color(0xFF2A1A00),
    background = Grafito,
    onBackground = TextoPrimario,
    surface = Superficie,
    onSurface = TextoPrimario,
    surfaceVariant = SuperficieAlta,
    onSurfaceVariant = TextoSecundario,
    outline = Linea,
    outlineVariant = Linea,
    error = Peligro,
    onError = Color(0xFF2A0000),
)

val FinanzasLight = lightColorScheme(
    primary = DivisaLocalOscura,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F1F8),
    onPrimaryContainer = Color(0xFF06323D),
    secondary = DivisaUsdOscura,
    onSecondary = Color.White,
    background = Nieve,
    onBackground = TextoPrimarioClaro,
    surface = Blanco,
    onSurface = TextoPrimarioClaro,
    surfaceVariant = SuperficieAltaClara,
    onSurfaceVariant = TextoSecundarioClaro,
    outline = LineaClara,
    outlineVariant = LineaClara,
    error = GastoClaro,
    onError = Color.White,
)

/** Color del punto/rail que identifica una divisa. */
fun currencyAccent(currency: String, dark: Boolean): Color = when (currency.uppercase()) {
    "USD" -> if (dark) DivisaUsd else DivisaUsdOscura
    else -> if (dark) DivisaLocal else DivisaLocalOscura
}
