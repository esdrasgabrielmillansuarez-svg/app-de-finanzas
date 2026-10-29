package com.suarezmillan.finanzas.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.ui.AmountEquivalence

/**
 * Línea de equivalencia en la otra divisa de referencia.
 *
 * Si la divisa base ya es USD o VES, no repite el mismo importe: muestra la
 * equivalencia en la otra. Si falta la cotización, lo dice; nunca inventa.
 */
@Composable
fun EquivalenceText(
    equivalence: AmountEquivalence,
    baseCurrency: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val parts = buildList {
        if (!baseCurrency.equals("USD", ignoreCase = true)) {
            equivalence.usdMinor?.let { add(formatEquivalent(it, "USD")) }
        }
        if (!baseCurrency.equals("VES", ignoreCase = true)) {
            equivalence.vesMinor?.let { add(formatEquivalent(it, "VES")) }
        }
    }
    Text(
        text = parts.joinToString(" · ").ifBlank { "Sin cotización para equivalencias" },
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 2,
    )
}

/** Nota única que explica con qué tasa se recalculan las equivalencias. */
@Composable
fun EquivalentRateNote(
    usdToVesRate: Double?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = if (usdToVesRate != null) {
            "Se recalcula con la tasa actual: 1 USD = ${MoneyFormat.formatRate(usdToVesRate)} VES"
        } else {
            "Sin cotización USD/VES: no se pueden calcular equivalencias."
        },
        modifier = modifier,
        style = style,
        color = color,
    )
}

private fun formatEquivalent(minor: Long, currency: String): String =
    if (minor < 0) "≈ ${MoneyFormat.formatSigned(minor, currency)}"
    else "≈ ${MoneyFormat.format(minor, currency)}"
