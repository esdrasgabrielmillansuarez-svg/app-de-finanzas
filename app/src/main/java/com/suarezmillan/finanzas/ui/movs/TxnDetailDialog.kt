package com.suarezmillan.finanzas.ui.movs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.ui.components.MoneyText
import com.suarezmillan.finanzas.ui.theme.Peligro
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val detailFormat = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale("es", "AR"))

/**
 * Detalle de un movimiento con las dos acciones que de verdad se usan:
 * **duplicar** (repetir el gasto) y **eliminar** (con deshacer en el snackbar).
 */
@Composable
fun TxnDetailDialog(
    txn: TxnEntity,
    categoryName: String?,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val whenText = detailFormat.format(
        Instant.ofEpochMilli(txn.occurredAt).atZone(ZoneId.systemDefault()),
    )
    val typeLabel = when (txn.type) {
        com.suarezmillan.finanzas.data.local.entity.TxnType.EXPENSE -> "Gasto"
        com.suarezmillan.finanzas.data.local.entity.TxnType.INCOME -> "Ingreso"
        com.suarezmillan.finanzas.data.local.entity.TxnType.TRANSFER -> "Transferencia"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = txn.merchant?.takeIf { it.isNotBlank() } ?: categoryName ?: typeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$typeLabel · $whenText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(14.dp))
                MoneyText(
                    minor = txn.amountMinor,
                    currency = txn.currency,
                    signed = true,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (txn.amountMinor < 0) Peligro else MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(10.dp))

                DetailRow("Categoría", categoryName ?: "Sin categoría")
                DetailRow("Divisa", txn.currency)
                txn.note?.takeIf { it.isNotBlank() }?.let { DetailRow("Nota", it) }
                txn.fxRateToBase?.let {
                    DetailRow(
                        "Tasa usada",
                        "1 ${txn.currency} = ${MoneyFormat.formatRate(it)} (base)",
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDelete) {
                        Text("Eliminar", color = Peligro)
                    }
                    TextButton(onClick = onDuplicate) { Text("Duplicar") }
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
