package com.suarezmillan.finanzas.ui.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.core.currency.LiveRate
import com.suarezmillan.finanzas.core.currency.VesReference
import com.suarezmillan.finanzas.core.currency.remote.RateRefreshState
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.ui.components.ManualRateDialog
import com.suarezmillan.finanzas.ui.components.timeAgo

/**
 * Ajustes → Tasas de cambio.
 *
 * El usuario elige la referencia del bolívar (BCV, paralelo o USDT) y puede
 * fijar a mano la equivalencia de **cada** referencia y del euro / peso
 * argentino. Regla única: manual > en vivo; si no hay dato, se indica.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesSettingsScreen(
    activeVesReference: VesReference,
    vesReferences: Map<VesReference, LiveRate>,
    vesManualRates: Map<VesReference, Double>,
    liveRates: Map<String, LiveRate>,
    manualRates: Map<String, Double>,
    refreshState: RateRefreshState,
    onBack: () -> Unit,
    onSelectVesReference: (VesReference) -> Unit,
    onSetVesManualRate: (VesReference, Double) -> Unit,
    onClearVesManualRate: (VesReference) -> Unit,
    onSetManualRate: (String, Double) -> Unit,
    onClearManualRate: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var editingVes by remember { mutableStateOf<VesReference?>(null) }
    var editingCurrency by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasas de cambio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                RefreshCard(state = refreshState, onRefresh = onRefresh)
            }

            item {
                SectionLabel("Bolívar (VES)")
            }

            items(VesReference.entries.size) { index ->
                val reference = VesReference.entries[index]
                VesReferenceRow(
                    reference = reference,
                    live = vesReferences[reference],
                    manual = vesManualRates[reference],
                    active = activeVesReference == reference,
                    onSelect = { onSelectVesReference(reference) },
                    onEdit = { editingVes = reference },
                    onClear = { onClearVesManualRate(reference) },
                )
            }

            item {
                Text(
                    "USDT no tiene una cotización pública estable en VES: se estima " +
                        "con el paralelo. Si tu exchange o casa de cambio da otro valor, " +
                        "fija la equivalencia a mano.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                SectionLabel("Euro y peso argentino")
            }

            item {
                val vesUnits = vesManualRates[activeVesReference]
                    ?: vesReferences[activeVesReference]?.unitsPerUsd
                val eurUnits = manualRates["EUR"] ?: liveRates["EUR"]?.unitsPerUsd
                val eurInVes = if (vesUnits != null && eurUnits != null && eurUnits > 0.0) {
                    vesUnits / eurUnits
                } else {
                    null
                }
                CurrencyRateRow(
                    code = "EUR",
                    label = "Euro",
                    referenceLabel = "BCE · Frankfurter",
                    live = liveRates["EUR"],
                    manual = manualRates["EUR"],
                    extraLine = eurInVes?.let {
                        "1 EUR ≈ ${MoneyFormat.formatRate(it)} VES (referencia activa)"
                    },
                    onEdit = { editingCurrency = "EUR" },
                    onClear = { onClearManualRate("EUR") },
                )
            }

            item {
                CurrencyRateRow(
                    code = "ARS",
                    label = "Peso argentino",
                    referenceLabel = "Oficial · dolarapi",
                    live = liveRates["ARS"],
                    manual = manualRates["ARS"],
                    onEdit = { editingCurrency = "ARS" },
                    onClear = { onClearManualRate("ARS") },
                )
            }

            item {
                Text(
                    "Prioridad: tu tasa manual > referencia en vivo. Si no hay dato, la " +
                        "app no inventa una tasa: puedes fijarla a mano. Las referencias " +
                        "se cachean 6 h y, si falla la red, se mantiene la última. Solo se " +
                        "hacen GET anónimos: movimientos y facturas nunca salen del teléfono.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
        }
    }

    editingVes?.let { reference ->
        val existing = vesManualRates[reference]
        ManualRateDialog(
            title = "Tasa manual · ${reference.label}",
            question = "¿Cuántos VES equivalen a ${reference.unitLabel}?",
            fieldLabel = "${reference.unitLabel} = VES",
            initialValue = existing?.toString() ?: "",
            onConfirm = { value ->
                onSetVesManualRate(reference, value)
                editingVes = null
            },
            onDismiss = { editingVes = null },
        )
    }

    editingCurrency?.let { code ->
        val existing = manualRates[code]
        ManualRateDialog(
            title = "Tasa manual · $code",
            question = "¿Cuántos $code equivalen a 1 USD?",
            fieldLabel = "1 USD = $code",
            initialValue = existing?.toString() ?: "",
            onConfirm = { value ->
                onSetManualRate(code, value)
                editingCurrency = null
            },
            onDismiss = { editingCurrency = null },
        )
    }
}

@Composable
private fun RefreshCard(state: RateRefreshState, onRefresh: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Referencias en vivo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "BCV, paralelo, USDT, euro y peso argentino.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onRefresh, enabled = !state.loading) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Actualizar")
                    }
                }
            }
            if (state.error != null) {
                Text(
                    state.error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun VesReferenceRow(
    reference: VesReference,
    live: LiveRate?,
    manual: Double?,
    active: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onClear: () -> Unit,
) {
    val value = manual ?: live?.unitsPerUsd

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (active) 2.dp else 1.dp,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = active, onClick = onSelect)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            reference.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (manual != null) Badge("manual") else if (live != null) Badge("live")
                        if (active) Badge("en uso")
                    }
                    Text(
                        reference.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = if (value != null) {
                    "${reference.unitLabel} = ${MoneyFormat.formatRate(value)} VES"
                } else {
                    "Sin dato · fija una tasa manual"
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
            Text(
                text = when {
                    manual != null && live != null ->
                        "manual · referencia en vivo: ${MoneyFormat.formatRate(live.unitsPerUsd)}"
                    manual != null -> "tasa manual"
                    live != null -> "${live.source} · ${timeAgo(live.fetchedAtMillis)}"
                    else -> "Actualiza o fija una tasa manual."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!active) {
                    TextButton(onClick = onSelect) { Text("Usar") }
                }
                TextButton(onClick = onEdit) {
                    Text(if (manual != null) "Editar" else "Tasa manual")
                }
                if (manual != null) {
                    TextButton(onClick = onClear) { Text("Quitar") }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRateRow(
    code: String,
    label: String,
    referenceLabel: String,
    live: LiveRate?,
    manual: Double?,
    extraLine: String? = null,
    onEdit: () -> Unit,
    onClear: () -> Unit,
) {
    val value = manual ?: live?.unitsPerUsd

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$label ($code)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (manual != null) Badge("manual") else if (live != null) Badge("live")
                    }
                    Text(
                        when {
                            manual != null && live != null ->
                                "manual · en vivo: ${MoneyFormat.formatRate(live.unitsPerUsd)}"
                            manual != null -> "fijada a mano"
                            live != null -> "$referenceLabel · ${timeAgo(live.fetchedAtMillis)}"
                            else -> "$referenceLabel · sin conexión"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = if (value != null) {
                    "1 USD = ${MoneyFormat.formatRate(value)} $code"
                } else {
                    "Sin dato · fija una tasa manual"
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (extraLine != null) {
                Text(
                    extraLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) {
                    Text(if (manual != null) "Editar" else "Tasa manual")
                }
                if (manual != null) {
                    TextButton(onClick = onClear) { Text("Quitar") }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Badge(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(start = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
