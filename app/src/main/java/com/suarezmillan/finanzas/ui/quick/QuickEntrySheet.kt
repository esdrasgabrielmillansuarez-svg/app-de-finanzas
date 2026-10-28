package com.suarezmillan.finanzas.ui.quick

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.core.receipt.CategoryHint
import com.suarezmillan.finanzas.data.local.entity.CategoryKind
import com.suarezmillan.finanzas.data.local.entity.TxnType
import com.suarezmillan.finanzas.ui.HomeUiState
import com.suarezmillan.finanzas.ui.ReceiptUiState

/**
 * Hoja de registro rápido: tipo, divisa, importe, categoría y factura, en una
 * sola pantalla. Objetivo del brief: < 3 s desde el lanzamiento.
 */
@Composable
fun QuickEntrySheet(
    state: HomeUiState,
    receipt: ReceiptUiState,
    onScan: (Uri) -> Unit,
    onClearReceipt: () -> Unit,
    onSave: (TxnType, Long, String, String, String?) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(TxnType.EXPENSE) }
    var currency by rememberSaveable { mutableStateOf(state.baseCurrency) }
    var expression by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }

    val categories = remember(state.categories, type) {
        val wanted = if (type == TxnType.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME
        state.categories.filter { it.kind == wanted }
    }

    LaunchedEffect(categories) {
        if (categories.none { it.id == categoryId }) {
            categoryId = categories.firstOrNull()?.id
        }
    }

    // Resultado del OCR: precarga importe/comercio/divisa/categoría, nunca guarda sola.
    LaunchedEffect(receipt) {
        val analysis = (receipt as? ReceiptUiState.Done)?.analysis ?: return@LaunchedEffect
        type = TxnType.EXPENSE
        analysis.totalMinor?.let { total ->
            expression = minorToExpression(total)
        }
        analysis.merchant?.let { merchant = it }
        analysis.currency
            ?.takeIf { it == state.baseCurrency || it == state.secondaryCurrency }
            ?.let { currency = it }
        analysis.categoryHint?.let { hint ->
            val suggested = hint.seedCategoryId()
            if (state.categories.any { it.id == suggested }) categoryId = suggested
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) onScan(uri)
    }

    val amountMinor = AmountParser.evaluateToMinor(expression)

    val press: (String) -> Unit = { key ->
        expression = when (key) {
            "⌫" -> expression.dropLast(1)
            "÷" -> appendOperator(expression, "/")
            "×" -> appendOperator(expression, "*")
            "−" -> appendOperator(expression, "-")
            "+" -> appendOperator(expression, "+")
            else -> if (expression.length >= 14) expression else expression + key
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == TxnType.EXPENSE,
                onClick = { type = TxnType.EXPENSE },
                label = { Text("Gasto") },
            )
            FilterChip(
                selected = type == TxnType.INCOME,
                onClick = { type = TxnType.INCOME },
                label = { Text("Ingreso") },
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ) {
                Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Factura")
            }
        }

        ReceiptBanner(
            receipt = receipt,
            fallbackCurrency = state.baseCurrency,
            onDiscard = onClearReceipt,
        )

        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CurrencyChip(
                currency = state.baseCurrency,
                selected = currency == state.baseCurrency,
                onClick = { currency = state.baseCurrency },
            )
            CurrencyChip(
                currency = state.secondaryCurrency,
                selected = currency == state.secondaryCurrency,
                onClick = { currency = state.secondaryCurrency },
            )
            Spacer(Modifier.weight(1f))
            if (currency != state.baseCurrency) {
                Text(
                    text = "1 ${state.secondaryCurrency} = ${"%.2f".format(state.rate)} ${state.baseCurrency}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = if (amountMinor == null) "0" else MoneyFormat.format(amountMinor, currency),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = expression.ifEmpty { "Escribe el importe" }
                .replace("*", "×")
                .replace("/", "÷")
                .replace("-", "−"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = category.id == categoryId,
                    onClick = { categoryId = category.id },
                    label = { Text("${category.emoji} ${category.name}") },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it.take(40) },
            label = { Text("¿En qué? (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(14.dp))

        Keypad(onKey = press)

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (expression.isNotEmpty()) {
                TextButton(onClick = { expression = "" }) { Text("Limpiar") }
            }
        }

        Spacer(Modifier.height(6.dp))

        Button(
            onClick = {
                val amount = amountMinor ?: return@Button
                val cat = categoryId ?: return@Button
                onSave(type, amount, currency, cat, merchant.ifBlank { null })
            },
            enabled = amountMinor != null && categoryId != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text("Guardar", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Estado del análisis de factura. Solo informa: el usuario confirma con
 * "Guardar". Si la confianza es baja, se dice explícitamente.
 */
@Composable
private fun ReceiptBanner(
    receipt: ReceiptUiState,
    fallbackCurrency: String,
    onDiscard: () -> Unit,
) {
    when (receipt) {
        ReceiptUiState.Idle -> Unit

        ReceiptUiState.Loading -> Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Leyendo la factura…", style = MaterialTheme.typography.labelMedium)
        }

        is ReceiptUiState.Failed -> Text(
            text = "No se pudo leer: ${receipt.message}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )

        is ReceiptUiState.Done -> {
            val analysis = receipt.analysis
            Card(
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Factura leída · ${(analysis.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onDiscard) { Text("Descartar") }
                    }
                    analysis.totalMinor?.let {
                        Text(
                            "Total detectado: ${MoneyFormat.format(it, analysis.currency ?: fallbackCurrency)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    analysis.date?.let {
                        Text("Fecha: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    analysis.merchant?.let {
                        Text("Comercio: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (analysis.warnings.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = analysis.warnings.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Revisa los datos antes de guardar.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyChip(currency: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(currency) },
    )
}

/** 4×4, teclas de 50 dp: el pulgar llega a todo con una mano. */
@Composable
private fun Keypad(onKey: (String) -> Unit) {
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

private fun appendOperator(expression: String, op: String): String = when {
    expression.isEmpty() -> if (op == "-") op else expression
    expression.last() in "+-*/" -> expression.dropLast(1) + op
    else -> expression + op
}

/** 210_000 → "2100" · 210_050 → "2100,50" (formato del teclado, no del display). */
private fun minorToExpression(minor: Long): String {
    val major = minor / 100
    val cents = (minor % 100).toInt()
    return if (cents == 0) major.toString() else "%d,%02d".format(major, cents)
}

/** Traduce la pista del OCR a las categorías sembradas en la base. */
private fun CategoryHint.seedCategoryId(): String = when (this) {
    CategoryHint.SUPER -> "cat-super"
    CategoryHint.FOOD -> "cat-food"
    CategoryHint.TRANSPORT -> "cat-transp"
    CategoryHint.HOME -> "cat-home"
    CategoryHint.FUN -> "cat-fun"
    CategoryHint.HEALTH -> "cat-health"
    CategoryHint.BILLS -> "cat-bills"
}
