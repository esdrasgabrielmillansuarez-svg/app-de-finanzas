package com.suarezmillan.finanzas.ui.divisas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.suarezmillan.finanzas.core.currency.CurrencyQuote
import com.suarezmillan.finanzas.core.currency.LiveRate
import com.suarezmillan.finanzas.core.currency.Region
import com.suarezmillan.finanzas.core.currency.VesReference
import com.suarezmillan.finanzas.core.currency.remote.RateRefreshState
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.ui.components.ManualRateDialog
import com.suarezmillan.finanzas.ui.components.timeAgo

/**
 * Catálogo completo de divisas, agrupado por región y con los países que usan
 * cada una. Cada fila muestra cuánto vale **1 unidad en USD y en EUR**.
 *
 * Referencias reales: BCV, paralelo y USDT para el bolívar; euro del BCE, peso
 * argentino oficial y una tabla global para el resto. Si una divisa no tiene
 * cotización, se dice explícitamente y se puede fijar una tasa manual. No hay
 * valores aproximados inventados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrenciesScreen(
    quotes: List<CurrencyQuote>,
    baseCurrency: String,
    secondaryCurrency: String,
    activeVesReference: VesReference,
    vesReferences: Map<VesReference, LiveRate>,
    vesManualRates: Map<VesReference, Double>,
    refreshState: RateRefreshState,
    onRefreshRate: () -> Unit,
    onSelectVesReference: (VesReference) -> Unit,
    onBack: () -> Unit,
    onSetBase: (String) -> Unit,
    onSetSecondary: (String) -> Unit,
    onSetManualRate: (String, Double) -> Unit,
    onClearManualRate: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<CurrencyQuote?>(null) }
    var manualTarget by remember { mutableStateOf<CurrencyQuote?>(null) }

    val filtered = remember(quotes, query) {
        val q = query.trim()
        if (q.isEmpty()) quotes
        else quotes.filter { quote ->
            quote.code.contains(q, ignoreCase = true) ||
                quote.name.contains(q, ignoreCase = true) ||
                quote.countries.any { it.name.contains(q, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Divisas") },
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
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                VesRateCard(
                    activeVesReference = activeVesReference,
                    vesReferences = vesReferences,
                    vesManualRates = vesManualRates,
                    state = refreshState,
                    onRefresh = onRefreshRate,
                    onSelectReference = onSelectVesReference,
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar país, divisa o código") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(
                    text = "Referencias reales: BCV, paralelo, USDT, EUR, ARS y tabla global " +
                        "(${quotes.size} divisas). Si una divisa no tiene cotización, " +
                        "se muestra sin tasa y puedes fijarla a mano.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Region.entries.forEach { region ->
                val regionQuotes = filtered.filter { it.region == region }
                if (regionQuotes.isEmpty()) return@forEach

                item(key = "region-${region.name}") {
                    Text(
                        text = region.label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                    )
                }

                items(regionQuotes, key = { it.code }) { quote ->
                    CurrencyRow(
                        quote = quote,
                        isBase = quote.code == baseCurrency,
                        isSecondary = quote.code == secondaryCurrency,
                        liveBadge = if (quote.code == "VES") activeVesReference.shortLabel else "live",
                        onClick = { selected = quote },
                    )
                }
            }
        }
    }

    selected?.let { quote ->
        CurrencyDetailDialog(
            quote = quote,
            isBase = quote.code == baseCurrency,
            isSecondary = quote.code == secondaryCurrency,
            onSetBase = {
                onSetBase(quote.code)
                selected = null
            },
            onSetSecondary = {
                onSetSecondary(quote.code)
                selected = null
            },
            onFixManual = {
                selected = null
                manualTarget = quote
            },
            onClearManual = {
                onClearManualRate(quote.code)
                selected = null
            },
            onDismiss = { selected = null },
        )
    }

    manualTarget?.let { quote ->
        ManualRateDialog(
            title = "Tasa manual",
            question = if (quote.code == "VES") {
                "¿Cuántos VES equivalen a ${activeVesReference.unitLabel}?"
            } else {
                "¿Cuántos ${quote.code} equivalen a 1 USD?"
            },
            fieldLabel = if (quote.code == "VES") {
                "${activeVesReference.unitLabel} = VES"
            } else {
                "1 USD = ${quote.code}"
            },
            onConfirm = { value ->
                onSetManualRate(quote.code, value)
                manualTarget = null
            },
            onDismiss = { manualTarget = null },
        )
    }
}

@Composable
private fun VesRateCard(
    activeVesReference: VesReference,
    vesReferences: Map<VesReference, LiveRate>,
    vesManualRates: Map<VesReference, Double>,
    state: RateRefreshState,
    onRefresh: () -> Unit,
    onSelectReference: (VesReference) -> Unit,
) {
    val live = vesReferences[activeVesReference]
    val manual = vesManualRates[activeVesReference]
    val value = manual ?: live?.unitsPerUsd
    val title = when (activeVesReference) {
        VesReference.BCV -> "Dólar BCV"
        VesReference.PARALELO -> "Dólar paralelo"
        VesReference.USDT -> "USDT en Venezuela"
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (manual != null) "$title · manual" else title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (value != null) {
                        Text(
                            "${activeVesReference.unitLabel} = ${MoneyFormat.formatRate(value)} VES",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (manual != null) {
                                "fijada a mano en Ajustes"
                            } else {
                                timeAgo(live?.fetchedAtMillis ?: 0L)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Sin cotización todavía",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Toca actualizar o fija una tasa manual.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VesReference.entries.forEach { reference ->
                    FilterChip(
                        selected = reference == activeVesReference,
                        onClick = { onSelectReference(reference) },
                        label = { Text(reference.label) },
                    )
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
            Text(
                "Fuentes: BCV, paralelo y USDT (estimado con el paralelo). " +
                    "Solo GET anónimos; movimientos y facturas no salen del teléfono.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun CurrencyRow(
    quote: CurrencyQuote,
    isBase: Boolean,
    isSecondary: Boolean,
    liveBadge: String,
    onClick: () -> Unit,
) {
    val flags = quote.countries.take(2).joinToString(" ") { it.flag }
    val countries = quote.countries.joinToString(" · ") { it.name }

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = flags.ifEmpty { "🏳️" }, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quote.code,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isBase) Badge("base")
                    if (isSecondary) Badge("2ª")
                    if (quote.isManual) Badge("manual")
                    if (quote.isLive) Badge(liveBadge)
                    if (!quote.isManual && !quote.isLive && quote.unitsPerUsd == null) {
                        Badge("sin tasa")
                    }
                }
                Text(
                    text = countries.ifEmpty { quote.name },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "1 ${quote.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val usd = quote.usdPerUnit
                val eur = quote.eurPerUnit
                if (usd == null && eur == null) {
                    Text(
                        text = "sin cotización",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "${MoneyFormat.formatRate(usd ?: 0.0)} USD",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${MoneyFormat.formatRate(eur ?: 0.0)} EUR",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
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

@Composable
private fun CurrencyDetailDialog(
    quote: CurrencyQuote,
    isBase: Boolean,
    isSecondary: Boolean,
    onSetBase: () -> Unit,
    onSetSecondary: () -> Unit,
    onFixManual: () -> Unit,
    onClearManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "${quote.code} · ${quote.name}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = quote.countries.joinToString(", ") { "${it.flag} ${it.name}" }.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                if (quote.unitsPerUsd == null) {
                    Text(
                        "Sin cotización disponible. Fija una tasa manual para esta divisa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    RateLine("1 ${quote.code}", "${MoneyFormat.formatRate(quote.usdPerUnit ?: 0.0)} USD")
                    RateLine("", "${MoneyFormat.formatRate(quote.eurPerUnit ?: 0.0)} EUR")
                    RateLine("1 USD", "${MoneyFormat.formatRate(quote.unitsPerUsd ?: 0.0)} ${quote.code}")
                }
                if (quote.code == "USDT") {
                    RateLine("1 USDT", "≈ 1 USD (stablecoin)")
                }
                if (quote.isManual) {
                    Text(
                        "Tasa manual activa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (quote.isLive && quote.liveUpdatedAtMillis != null) {
                    Text(
                        "Cotización ${quote.liveSource ?: "en vivo"} · " +
                            timeAgo(quote.liveUpdatedAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    if (!isBase && !isSecondary) {
                        DialogAction("Usar como base", onSetBase)
                    }
                    if (!isSecondary && !isBase) {
                        DialogAction("Usar como secundaria", onSetSecondary)
                    }
                    DialogAction(
                        text = if (quote.isManual) "Cambiar tasa manual" else "Fijar tasa manual",
                        onClick = onFixManual,
                    )
                    if (quote.isManual) {
                        DialogAction("Quitar tasa manual", onClearManual)
                    }
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}

@Composable
private fun DialogAction(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
    }
}

@Composable
private fun RateLine(left: String, right: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = left,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = right, style = MaterialTheme.typography.bodyMedium)
    }
}
