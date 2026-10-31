package com.suarezmillan.finanzas.ui.cambio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.core.currency.CurrencyRates
import com.suarezmillan.finanzas.core.currency.ExchangeMath
import com.suarezmillan.finanzas.core.currency.LiveRate
import com.suarezmillan.finanzas.core.currency.VesReference
import com.suarezmillan.finanzas.core.currency.remote.RateRefreshState
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.ui.components.AmountKeypad
import com.suarezmillan.finanzas.ui.components.applyAmountKey
import com.suarezmillan.finanzas.ui.components.timeAgo
import com.suarezmillan.finanzas.ui.quick.AmountParser

/**
 * Calculadora de cambio dólar ↔ bolívar, al estilo de una app de cambio.
 *
 * No es un registro: solo calcula con la tasa que ya usa la app (manual > en
 * vivo), muestra la fuente y el sentido de la operación. Si falta la tasa, no
 * inventa una equivalencia: invita a actualizar o fijarla a mano.
 *
 * El teclado es el mismo del registro rápido (aritmética inline incluida) para
 * que el gesto sea consistente y no aparezca el teclado del sistema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeCalculatorScreen(
    activeVesReference: VesReference,
    vesReferences: Map<VesReference, LiveRate>,
    vesManualRates: Map<VesReference, Double>,
    refreshState: RateRefreshState,
    rate: (from: String, to: String) -> Double?,
    onSelectVesReference: (VesReference) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    var from by rememberSaveable { mutableStateOf(CurrencyRates.USD) }
    var expression by rememberSaveable { mutableStateOf("") }

    val to = if (from == CurrencyRates.USD) CurrencyRates.VES else CurrencyRates.USD
    val amountMinor = AmountParser.evaluateToMinor(expression)
    val conversionRate = rate(from, to)
    val resultMinor = ExchangeMath.convert(amountMinor, conversionRate)
    val usdToVes = rate(CurrencyRates.USD, CurrencyRates.VES)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculadora de cambio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 24.dp),
        ) {
            ReferenceCard(
                active = activeVesReference,
                references = vesReferences,
                manualRates = vesManualRates,
                refreshState = refreshState,
                usdToVes = usdToVes,
                onSelect = onSelectVesReference,
                onRefresh = onRefresh,
            )

            Spacer(Modifier.height(14.dp))

            ConverterCard(
                from = from,
                to = to,
                amountMinor = amountMinor,
                resultMinor = resultMinor,
                expression = expression,
                usdToVes = usdToVes,
                onSwap = { from = to },
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Atajos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (expression.isNotEmpty()) {
                    TextButton(onClick = { expression = "" }) { Text("Limpiar") }
                }
            }

            PresetRow(
                currency = from,
                amountMinor = amountMinor,
                onPick = { expression = MoneyFormat.editInput(it) },
            )

            Spacer(Modifier.height(14.dp))

            AmountKeypad(onKey = { key -> expression = applyAmountKey(expression, key) })

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Solo calcula: no crea movimientos ni guarda nada. " +
                    "La tasa es la misma que ves en el resto de la app.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReferenceCard(
    active: VesReference,
    references: Map<VesReference, LiveRate>,
    manualRates: Map<VesReference, Double>,
    refreshState: RateRefreshState,
    usdToVes: Double?,
    onSelect: (VesReference) -> Unit,
    onRefresh: () -> Unit,
) {
    val manual = manualRates[active]
    val live = references[active]
    val source = when {
        manual != null -> "${active.label} · fija a mano"
        live != null -> "${live.source} · ${timeAgo(live.fetchedAtMillis)}"
        else -> "Sin cotización: actualiza o fija una tasa manual en Divisas."
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Dólar de hoy",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = usdToVes?.let {
                            "${active.unitLabel} = ${MoneyFormat.formatRate(it)} VES"
                        } ?: "Sin tasa disponible",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onRefresh, enabled = !refreshState.loading) {
                    if (refreshState.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Actualizar")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VesReference.entries.forEach { reference ->
                    FilterChip(
                        selected = reference == active,
                        onClick = { onSelect(reference) },
                        label = { Text(reference.label) },
                    )
                }
            }

            if (refreshState.error != null) {
                Text(
                    text = refreshState.error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ConverterCard(
    from: String,
    to: String,
    amountMinor: Long?,
    resultMinor: Long?,
    expression: String,
    usdToVes: Double?,
    onSwap: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                "Tengo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = amountMinor?.let { MoneyFormat.format(it, from) } ?: "0",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                CurrencyTag(from)
            }
            Text(
                text = expression.ifEmpty { "Escribe el monto" }
                    .replace("*", "×")
                    .replace("/", "÷")
                    .replace("-", "−"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalDivider(Modifier.fillMaxWidth())
                Surface(
                    onClick = onSwap,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SwapVert,
                            contentDescription = "Cambiar sentido",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Text(
                "Recibes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = resultMinor?.let { MoneyFormat.format(it, to) } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (resultMinor == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                CurrencyTag(to)
            }
            Text(
                text = when {
                    amountMinor == null -> "Escribe el monto para ver la equivalencia."
                    resultMinor == null -> "Sin tasa: no se inventa una equivalencia."
                    from == CurrencyRates.USD && usdToVes != null ->
                        "1 USD = ${MoneyFormat.formatRate(usdToVes)} VES"
                    else -> "1 USD = ${usdToVes?.let { MoneyFormat.formatRate(it) } ?: "—"} VES"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PresetRow(
    currency: String,
    amountMinor: Long?,
    onPick: (Long) -> Unit,
) {
    val units = if (currency == CurrencyRates.USD) {
        listOf(1L, 5L, 10L, 20L, 50L, 100L)
    } else {
        listOf(1_000L, 5_000L, 10_000L, 20_000L, 50_000L, 100_000L)
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        units.forEach { unit ->
            val minor = unit * 100L
            FilterChip(
                selected = amountMinor == minor,
                onClick = { onPick(minor) },
                label = {
                    Text(
                        if (currency == CurrencyRates.USD) {
                            "$${MoneyFormat.formatRate(unit.toDouble())}"
                        } else {
                            MoneyFormat.formatRate(unit.toDouble())
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun CurrencyTag(code: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (code == CurrencyRates.USD) "US$" else "Bs.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = code,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
