package com.suarezmillan.finanzas.ui.receipt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.core.receipt.ReceiptDraft
import com.suarezmillan.finanzas.core.receipt.ReceiptSource
import com.suarezmillan.finanzas.core.receipt.ReceiptStatus
import com.suarezmillan.finanzas.core.receipt.draftsScanning
import com.suarezmillan.finanzas.core.receipt.draftsToSave
import com.suarezmillan.finanzas.data.local.entity.CategoryEntity
import com.suarezmillan.finanzas.data.local.entity.CategoryKind
import com.suarezmillan.finanzas.ui.components.CategoryDialog
import com.suarezmillan.finanzas.ui.quick.AmountParser

/**
 * Bandeja de facturas: se cargan varias (cámara o galería), se leen en cola y
 * aquí se corrigen y confirman. Nada se guarda sin pulsar el botón final.
 */
@Composable
fun ReceiptBatchSheet(
    drafts: List<ReceiptDraft>,
    categories: List<CategoryEntity>,
    baseCurrency: String,
    secondaryCurrency: String,
    onAmount: (String, Long?) -> Unit,
    onMerchant: (String, String) -> Unit,
    onCurrency: (String, String) -> Unit,
    onCategory: (String, String) -> Unit,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCreateCategory: (String, String) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
) {
    val ready = remember(drafts) { draftsToSave(drafts) }
    val scanning = remember(drafts) { draftsScanning(drafts) }

    // Al crear una categoría desde una tarjeta, se asigna en cuanto aparece.
    var pendingCategoryFor by remember { mutableStateOf<String?>(null) }
    var pendingCategoryName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(categories, pendingCategoryName) {
        val name = pendingCategoryName ?: return@LaunchedEffect
        val draftId = pendingCategoryFor ?: return@LaunchedEffect
        val created = categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (created != null) {
            onCategory(draftId, created.id)
            pendingCategoryName = null
            pendingCategoryFor = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Facturas", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = when {
                        drafts.isEmpty() -> "Sin fotos en la bandeja"
                        scanning > 0 -> "$scanning leyéndose · ${drafts.size} en total"
                        else -> "${drafts.size} en la bandeja · ${ready.size} listas"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (drafts.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Descartar todo") }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (drafts.isEmpty()) {
            Text(
                text = "Toma una foto o elige varias de la galería. Se leen en el teléfono, " +
                    "sin subir nada a internet, y se procesan una detrás de otra.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(drafts, key = { _, draft -> draft.id }) { index, draft ->
                    DraftCard(
                        index = index,
                        draft = draft,
                        categories = categories,
                        baseCurrency = baseCurrency,
                        secondaryCurrency = secondaryCurrency,
                        onAmount = onAmount,
                        onMerchant = onMerchant,
                        onCurrency = onCurrency,
                        onCategory = onCategory,
                        onToggle = onToggle,
                        onRemove = onRemove,
                        onNewCategory = { name, emoji ->
                            pendingCategoryFor = draft.id
                            pendingCategoryName = name
                            onCreateCategory(name, emoji)
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCamera) {
                Icon(
                    Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Cámara")
            }
            OutlinedButton(onClick = onGallery) {
                Icon(
                    Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Galería")
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onSave,
            enabled = ready.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(
                text = when (ready.size) {
                    0 -> "Guardar"
                    1 -> "Guardar 1 gasto"
                    else -> "Guardar ${ready.size} gastos"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }

        val pending = drafts.count { it.needsReview }
        if (pending > 0) {
            Text(
                text = "$pending sin importe o categoría: complétalas para poder guardarlas.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun DraftCard(
    index: Int,
    draft: ReceiptDraft,
    categories: List<CategoryEntity>,
    baseCurrency: String,
    secondaryCurrency: String,
    onAmount: (String, Long?) -> Unit,
    onMerchant: (String, String) -> Unit,
    onCurrency: (String, String) -> Unit,
    onCategory: (String, String) -> Unit,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onNewCategory: (String, String) -> Unit,
) {
    var showCategoryDialog by rememberSaveable(draft.id) { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val category = categories.firstOrNull { it.id == draft.categoryId }

    // El texto se conserva mientras se teclea; el OCR solo rellena si nadie lo tocó.
    var amountText by rememberSaveable(draft.id) {
        mutableStateOf(draft.amountMinor?.let { MoneyFormat.editInput(it) }.orEmpty())
    }
    var amountTouched by rememberSaveable(draft.id) { mutableStateOf(false) }
    LaunchedEffect(draft.amountMinor) {
        if (!amountTouched) {
            amountText = draft.amountMinor?.let { MoneyFormat.editInput(it) }.orEmpty()
        }
    }

    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (draft.source == ReceiptSource.CAMERA) {
                        Icons.Rounded.PhotoCamera
                    } else {
                        Icons.Rounded.PhotoLibrary
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = draft.merchant?.takeIf { it.isNotBlank() } ?: "Factura ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Checkbox(
                    checked = draft.include && draft.hasRequiredData,
                    onCheckedChange = { onToggle(draft.id) },
                    enabled = draft.hasRequiredData,
                )
                IconButton(onClick = { onRemove(draft.id) }) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Quitar factura",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (draft.status) {
                ReceiptStatus.SCANNING -> {
                    Text(
                        text = "Leyendo la factura…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                ReceiptStatus.FAILED -> Text(
                    text = "No se pudo leer: ${draft.error ?: "imagen no reconocida"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                ReceiptStatus.READY -> {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { raw ->
                            val clean = raw
                                .filter { it.isDigit() || it == ',' || it == '.' || it in "+-*/" }
                                .take(14)
                            amountText = clean
                            amountTouched = true
                            onAmount(draft.id, AmountParser.evaluateToMinor(clean))
                        },
                        label = { Text("Importe (${draft.currency})") },
                        singleLine = true,
                        isError = amountTouched && draft.amountMinor == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = draft.currency == baseCurrency,
                            onClick = { onCurrency(draft.id, baseCurrency) },
                            label = { Text(baseCurrency) },
                        )
                        if (secondaryCurrency != baseCurrency) {
                            FilterChip(
                                selected = draft.currency == secondaryCurrency,
                                onClick = { onCurrency(draft.id, secondaryCurrency) },
                                label = { Text(secondaryCurrency) },
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = draft.merchant.orEmpty(),
                        onValueChange = { onMerchant(draft.id, it) },
                        label = { Text("Comercio (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                    )

                    Spacer(Modifier.height(8.dp))

                    Box {
                        OutlinedButton(onClick = { menuOpen = true }) {
                            Text(
                                text = category?.let { "${it.emoji} ${it.name}" }
                                    ?: "Elegir categoría",
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            categories.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${option.emoji} ${option.name}") },
                                    onClick = {
                                        menuOpen = false
                                        onCategory(draft.id, option.id)
                                    },
                                )
                            }
                            if (categories.isNotEmpty()) HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ Nueva categoría") },
                                onClick = {
                                    menuOpen = false
                                    showCategoryDialog = true
                                },
                            )
                        }
                    }

                    if (draft.warnings.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = draft.warnings.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Leída al ${(draft.confidence * 100).toInt()}%. Revisa antes de guardar.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategoryDialog(
            kind = CategoryKind.EXPENSE,
            onDismiss = { showCategoryDialog = false },
            onConfirm = { name, emoji ->
                showCategoryDialog = false
                onNewCategory(name, emoji)
            },
        )
    }
}
