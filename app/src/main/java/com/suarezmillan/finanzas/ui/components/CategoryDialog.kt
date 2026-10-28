package com.suarezmillan.finanzas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.data.local.entity.CategoryKind

private val EMOJIS = listOf(
    "💸", "🛒", "🍔", "🚕", "🏠", "🎬", "💊", "💡", "📱", "🎓", "💵", "🧑‍💻", "🎁", "🐾",
)

/**
 * Crea una categoría propia. La app no siembra categorías: el usuario decide
 * nombre, icono y tipo, y desde ese momento la categoría es suya.
 */
@Composable
fun CategoryDialog(
    kind: CategoryKind,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable {
        mutableStateOf(if (kind == CategoryKind.INCOME) "💵" else "💸")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (kind == CategoryKind.INCOME) "Nueva categoría de ingreso"
                else "Nueva categoría de gasto",
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text("Icono", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(EMOJIS) { option ->
                        FilterChip(
                            selected = option == emoji,
                            onClick = { emoji = option },
                            label = { Text(option) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), emoji) },
                enabled = name.isNotBlank(),
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
