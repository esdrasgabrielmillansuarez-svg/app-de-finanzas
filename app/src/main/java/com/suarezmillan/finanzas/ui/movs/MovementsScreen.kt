package com.suarezmillan.finanzas.ui.movs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.data.local.entity.CategoryEntity
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import com.suarezmillan.finanzas.ui.home.TxnRow

/**
 * Bandeja de movimientos con búsqueda y filtros de tipo/divisa.
 * Cada fila abre el detalle (duplicar / eliminar).
 */
@Composable
fun MovementsScreen(
    txns: List<TxnEntity>,
    categories: List<CategoryEntity>,
    onTxnClick: (TxnEntity) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf<TxnType?>(null) }
    var currencyFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    val currencies = remember(txns) { txns.map { it.currency }.distinct().sorted() }

    val filtered = remember(txns, query, typeFilter, currencyFilter, categoryNames) {
        val q = query.trim()
        txns.filter { txn ->
            val matchesType = typeFilter == null || txn.type == typeFilter
            val matchesCurrency = currencyFilter == null || txn.currency == currencyFilter
            val matchesQuery = q.isEmpty() ||
                txn.merchant?.contains(q, ignoreCase = true) == true ||
                txn.note?.contains(q, ignoreCase = true) == true ||
                categoryNames[txn.categoryId]?.contains(q, ignoreCase = true) == true
            matchesType && matchesCurrency && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Column {
                Text("Movimientos", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = when {
                        txns.isEmpty() -> "todavía no hay nada"
                        filtered.size == txns.size -> "${txns.size} en total"
                        else -> "${filtered.size} de ${txns.size}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar comercio, nota o categoría") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item {
                    FilterChip(
                        selected = typeFilter == null,
                        onClick = { typeFilter = null },
                        label = { Text("Todo") },
                    )
                }
                item {
                    FilterChip(
                        selected = typeFilter == TxnType.EXPENSE,
                        onClick = { typeFilter = TxnType.EXPENSE },
                        label = { Text("Gastos") },
                    )
                }
                item {
                    FilterChip(
                        selected = typeFilter == TxnType.INCOME,
                        onClick = { typeFilter = TxnType.INCOME },
                        label = { Text("Ingresos") },
                    )
                }
                if (currencies.size > 1) {
                    items(currencies, key = { "cur-$it" }) { code ->
                        FilterChip(
                            selected = currencyFilter == code,
                            onClick = { currencyFilter = if (currencyFilter == code) null else code },
                            label = { Text(code) },
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(
                        text = if (txns.isEmpty()) {
                            "Registra tu primer movimiento con el botón +."
                        } else {
                            "Nada coincide con el filtro."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { txn ->
            TxnRow(txn = txn, onClick = { onTxnClick(txn) })
        }
    }
}
