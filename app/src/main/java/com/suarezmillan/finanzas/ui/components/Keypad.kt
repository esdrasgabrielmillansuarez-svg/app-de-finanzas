package com.suarezmillan.finanzas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Teclado 4×4 para importes: dígitos, coma y aritmética inline.
 *
 * Se comparte entre el registro rápido y la calculadora de cambio para que
 * ambas pantallas se sientan igual. Cada tecla mide 50 dp: el pulgar llega a
 * todo con una mano.
 */
@Composable
fun AmountKeypad(onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("7", "8", "9", "−"),
        listOf(",", "0", "⌫", "+"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val isDigit = key.first().isDigit() || key == ","
                    Surface(
                        onClick = { onKey(key) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isDigit) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDigit) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Añade un operador sin permitir dos seguidos ni empezar con `+`, `*` o `/`. */
fun appendAmountOperator(expression: String, op: String): String = when {
    expression.isEmpty() -> if (op == "-") op else expression
    expression.last() in "+-*/" -> expression.dropLast(1) + op
    else -> expression + op
}

/** Aplica una tecla del teclado a la expresión, con el mismo tope de longitud. */
fun applyAmountKey(expression: String, key: String): String = when (key) {
    "⌫" -> expression.dropLast(1)
    "÷" -> appendAmountOperator(expression, "/")
    "×" -> appendAmountOperator(expression, "*")
    "−" -> appendAmountOperator(expression, "-")
    "+" -> appendAmountOperator(expression, "+")
    else -> if (expression.length >= 14) expression else expression + key
}
