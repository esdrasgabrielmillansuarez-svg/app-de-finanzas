package com.suarezmillan.finanzas.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.ui.HomeUiState
import com.suarezmillan.finanzas.ui.components.ConfirmDialog
import com.suarezmillan.finanzas.ui.theme.Peligro

@Composable
fun SettingsScreen(
    state: HomeUiState,
    userName: String,
    userEmail: String,
    movementCount: Int,
    onOpenCurrencies: () -> Unit,
    onExportCsv: () -> Unit,
    onClearMovements: () -> Unit,
    onLogout: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Ajustes", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Section("Perfil") {
                SettingRow("Nombre", userName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("Correo", userEmail)
                Text(
                    "Perfil local: no hay servidor ni recuperación por correo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(onClick = onLogout, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Cerrar sesión", color = Peligro)
                }
            }
        }

        item {
            Section("Divisas") {
                SettingRow("Base", state.baseCurrency)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("Secundaria", state.secondaryCurrency)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    label = "Tasa",
                    value = "1 ${state.secondaryCurrency} = " +
                        "${MoneyFormat.formatRate(state.rate)} ${state.baseCurrency}",
                    badge = if (state.rateIsManual) "manual" else "auto",
                )
                TextButton(onClick = onOpenCurrencies, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Ver todas las divisas y sus equivalencias")
                }
                Text(
                    "La tasa se guarda por movimiento: cambiarla no reescribe el pasado.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Section("Datos") {
                SettingRow("Movimientos", movementCount.toString())
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("Almacenamiento", "local")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("Nube", "desactivada")

                TextButton(onClick = onExportCsv, modifier = Modifier.padding(top = 6.dp)) {
                    Text("Exportar movimientos (CSV)")
                }
                TextButton(onClick = { confirmClear = true }, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        "Borrar todos los movimientos",
                        color = Peligro,
                        textAlign = TextAlign.Start,
                    )
                }
                Text(
                    "Esta versión no declara permiso de INTERNET: tus datos no salen del teléfono. " +
                        "El CSV se comparte solo cuando tú eliges el destino.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            Section("Seguridad") {
                SettingRow("Contraseña", "PBKDF2 local")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("Bloqueo biométrico", "pendiente")
                Text(
                    "La contraseña se guarda con sal y 120.000 iteraciones; nunca en claro. " +
                        "El desbloqueo con huella llega en el próximo sprint.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item {
            Text(
                "Finanzas 0.2.0 · offline-first",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "¿Borrar todos los movimientos?",
            text = "Se eliminan $movementCount movimientos de este teléfono. " +
                "Los sobres y los pagos programados se conservan. No se puede deshacer.",
            confirmLabel = "Borrar todo",
            destructive = true,
            onConfirm = {
                onClearMovements()
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

/** Fila de ajuste: etiqueta a la izquierda, valor y badge opcional a la derecha. */
@Composable
private fun SettingRow(label: String, value: String, badge: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        if (badge != null) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}
