package com.suarezmillan.finanzas.ui.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.core.branding.Branding
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.core.settings.ThemeMode
import com.suarezmillan.finanzas.core.settings.ThemePreset
import com.suarezmillan.finanzas.ui.HomeUiState
import com.suarezmillan.finanzas.ui.components.ConfirmDialog
import com.suarezmillan.finanzas.ui.theme.Peligro
import com.suarezmillan.finanzas.ui.theme.presetSwatch

@Composable
fun SettingsScreen(
    state: HomeUiState,
    userName: String,
    userEmail: String,
    movementCount: Int,
    themeMode: ThemeMode,
    themePreset: ThemePreset,
    remindersEnabled: Boolean,
    vesReferenceLabel: String,
    onThemeMode: (ThemeMode) -> Unit,
    onThemePreset: (ThemePreset) -> Unit,
    onReminders: (Boolean) -> Unit,
    onOpenCurrencies: () -> Unit,
    onOpenRates: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onClearMovements: () -> Unit,
    onLogout: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

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
            Section("Apariencia") {
                Text(
                    "Modo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeMode(mode) },
                            label = { Text(mode.label, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(
                    "Tema",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp),
                )
                ThemePreset.entries.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEach { preset ->
                            PresetCell(
                                preset = preset,
                                selected = themePreset == preset,
                                dark = dark,
                                modifier = Modifier.weight(1f),
                                onClick = { onThemePreset(preset) },
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Text(
                    "El tema tiñe la marca y las superficies: divisas y flujos conservan su color.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
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
                    value = if (state.rate > 0.0) {
                        "1 ${state.secondaryCurrency} = " +
                            "${MoneyFormat.formatRate(state.rate)} ${state.baseCurrency}"
                    } else {
                        "Sin tasa: fija una manual"
                    },
                    badge = when {
                        state.rate <= 0.0 -> "sin tasa"
                        state.rateIsManual -> "manual"
                        else -> "auto"
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("Referencia VES", vesReferenceLabel)
                TextButton(onClick = onOpenCurrencies, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Ver todas las divisas y sus equivalencias")
                }
                TextButton(onClick = onOpenRates, modifier = Modifier.padding(top = 2.dp)) {
                    Text("Tasas de cambio: BCV, paralelo, USDT, EUR, ARS y tabla global")
                }
                Text(
                    "La tasa se guarda por movimiento: cambiarla no reescribe el pasado.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Section("Recordatorios") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Avisos de vencimientos", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Una notificación al día si hay pagos cerca.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = remindersEnabled, onCheckedChange = onReminders)
                }
                Text(
                    "Se revisa a las 9:00 con WorkManager, sin conexión. " +
                        "Podés desactivarlo cuando quieras.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item {
            Section("Datos") {
                SettingRow("Movimientos", movementCount.toString())
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("Almacenamiento", "local")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("Conexión", "solo cotizaciones")

                TextButton(onClick = onExportCsv, modifier = Modifier.padding(top = 6.dp)) {
                    Text("Exportar movimientos (CSV)")
                }
                TextButton(onClick = onExportPdf, modifier = Modifier.padding(top = 2.dp)) {
                    Text("Generar reporte PDF del mes")
                }
                Text(
                    "El PDF incluye el resumen del mes, los movimientos y el estado de " +
                        "los sobres. Se comparte con el selector del sistema, igual que el CSV.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                TextButton(onClick = { confirmClear = true }, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        "Borrar todos los movimientos",
                        color = Peligro,
                        textAlign = TextAlign.Start,
                    )
                }
                Text(
                    "La app solo se conecta para traer referencias públicas de cambio " +
                        "(BCV, paralelo, EUR, ARS y tabla global). Movimientos, facturas y " +
                        "perfil nunca salen del teléfono. El CSV y el PDF se comparten solo " +
                        "cuando tú eliges el destino.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            Section("Seguridad") {
                SettingRow("Contraseña", "PBKDF2 local")
                Text(
                    "La contraseña se guarda con sal y 120.000 iteraciones; nunca en claro. " +
                        "El perfil y los movimientos se quedan en este teléfono.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item {
            Section("Acerca de") {
                SettingRow("Creador", Branding.CREATOR)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("Versión", Branding.VERSION)
                Text(
                    "Finanzas es una app local, sin cuentas en la nube. " +
                        "Hecha por ${Branding.CREATOR}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item {
            Text(
                "${Branding.APP_NAME} ${Branding.VERSION} · ${Branding.CREDIT}",
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

/** Muestra de tema: círculo de acento + nombre, seleccionable. */
@Composable
private fun PresetCell(
    preset: ThemePreset,
    selected: Boolean,
    dark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = presetSwatch(preset, dark),
            border = BorderStroke(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.size(34.dp),
        ) {}
        Text(
            text = preset.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
