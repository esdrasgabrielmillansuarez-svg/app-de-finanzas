package com.suarezmillan.finanzas.ui.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.data.local.entity.CategoryEntity
import com.suarezmillan.finanzas.data.local.entity.CategoryKind
import com.suarezmillan.finanzas.data.local.entity.Recurrence
import com.suarezmillan.finanzas.data.local.entity.ScheduledPaymentEntity
import com.suarezmillan.finanzas.data.local.model.EnvelopeProgress
import com.suarezmillan.finanzas.ui.AmountEquivalence
import com.suarezmillan.finanzas.ui.HomeUiState
import com.suarezmillan.finanzas.ui.components.CategoryDialog
import com.suarezmillan.finanzas.ui.components.EquivalenceText
import com.suarezmillan.finanzas.ui.components.EquivalentRateNote
import com.suarezmillan.finanzas.ui.components.MoneyText
import com.suarezmillan.finanzas.ui.quick.AmountParser
import com.suarezmillan.finanzas.ui.theme.Alerta
import com.suarezmillan.finanzas.ui.theme.Peligro
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("es", "AR"))

@Composable
fun PlanScreen(
    state: HomeUiState,
    payments: List<ScheduledPaymentEntity>,
    onSetLimit: (categoryId: String, limitMinor: Long) -> Unit,
    onCreateCategory: (name: String, emoji: String, kind: CategoryKind) -> Unit,
    onAddPayment: (String, Long, Long, Recurrence, String?) -> Unit,
    onPay: (ScheduledPaymentEntity) -> Unit,
    onDeletePayment: (ScheduledPaymentEntity) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Sobres") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Pagos") })
        }

        Box(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> EnvelopesTab(
                    state = state,
                    onSetLimit = onSetLimit,
                    onCreateCategory = onCreateCategory,
                )
                else -> PaymentsTab(
                    state = state,
                    payments = payments,
                    onAddPayment = onAddPayment,
                    onPay = onPay,
                    onDeletePayment = onDeletePayment,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ sobres

@Composable
private fun EnvelopesTab(
    state: HomeUiState,
    onSetLimit: (String, Long) -> Unit,
    onCreateCategory: (String, String, CategoryKind) -> Unit,
) {
    val byCategory = remember(state.envelopes) { state.envelopes.associateBy { it.categoryId } }
    val categories = state.categories.filter { it.kind == CategoryKind.EXPENSE }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SummaryCard(state)
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Categorías",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showCategoryDialog = true }) {
                    Text("+ Nueva")
                }
            }
        }

        if (categories.isEmpty()) {
            item {
                Text(
                    "No hay categorías de gasto. Crea una para asignarle un sobre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                "Toca una categoría para asignarle un límite mensual.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        items(categories, key = { it.id }) { category ->
            EnvelopeRow(
                category = category,
                progress = byCategory[category.id],
                equivalence = state.envelopeEquivalents[category.id],
                onClick = { editing = category },
            )
        }
    }

    editing?.let { category ->
        EnvelopeDialog(
            category = category,
            current = byCategory[category.id],
            currency = state.baseCurrency,
            onConfirm = { limit ->
                onSetLimit(category.id, limit)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    if (showCategoryDialog) {
        CategoryDialog(
            kind = CategoryKind.EXPENSE,
            onDismiss = { showCategoryDialog = false },
            onConfirm = { name, emoji ->
                showCategoryDialog = false
                onCreateCategory(name, emoji, CategoryKind.EXPENSE)
            },
        )
    }
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Te queda",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.hasMonthActivity) {
                        MoneyText(
                            minor = state.remainingMinor,
                            currency = state.baseCurrency,
                            signed = state.remainingMinor < 0L,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        EquivalenceText(
                            equivalence = state.remainingEquivalent,
                            baseCurrency = state.baseCurrency,
                        )
                        if (!state.hasIncome && state.spentMinor > 0L) {
                            Text(
                                "Sin ingresos registrados este mes.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        Text(
                            "Sin ingresos ni gastos",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.hasMonthActivity) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "gastado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        MoneyText(
                            minor = state.spentMinor,
                            currency = state.baseCurrency,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        EquivalenceText(
                            equivalence = state.spentEquivalent,
                            baseCurrency = state.baseCurrency,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            if (state.hasIncome) {
                LinearProgressIndicator(
                    progress = { state.usedRatio },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ingresos del mes: ${MoneyFormat.format(state.incomeMinor, state.baseCurrency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    if (state.spentMinor > 0L) {
                        "Sin ingresos registrados: el disponible no se puede calcular."
                    } else {
                        "Registra un ingreso para calcular tu disponible del mes."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.budgetLimitMinor > 0L) {
                Text(
                    "Límite de sobres: ${MoneyFormat.format(state.budgetLimitMinor, state.baseCurrency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EquivalentRateNote(usdToVesRate = state.usdToVesRate)
            if (state.missingRateExpenses > 0) {
                Text(
                    "${state.missingRateExpenses} gasto(s) del mes sin cotización no se sumaron.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EnvelopeRow(
    category: CategoryEntity,
    progress: EnvelopeProgress?,
    equivalence: AmountEquivalence?,
    onClick: () -> Unit,
) {
    val accent = when {
        progress == null -> MaterialTheme.colorScheme.outline
        progress.isOver -> Peligro
        progress.isWarning -> Alerta
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category.emoji)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (progress == null) {
                    Text(
                        "sin límite",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        MoneyText(
                            minor = progress.remainingMinor,
                            currency = progress.currency,
                            signed = progress.remainingMinor < 0L,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (progress.isOver) Peligro else MaterialTheme.colorScheme.onSurface,
                        )
                        equivalence?.let {
                            EquivalenceText(
                                equivalence = it,
                                baseCurrency = progress.currency,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
            if (progress != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.usedRatio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${MoneyFormat.format(progress.spentMinor, progress.currency)} de " +
                        MoneyFormat.format(progress.limitMinor, progress.currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ pagos

@Composable
private fun PaymentsTab(
    state: HomeUiState,
    payments: List<ScheduledPaymentEntity>,
    onAddPayment: (String, Long, Long, Recurrence, String?) -> Unit,
    onPay: (ScheduledPaymentEntity) -> Unit,
    onDeletePayment: (ScheduledPaymentEntity) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Próximos pagos", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Marcar como pagado crea el gasto del mes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { adding = true }) { Text("Añadir") }
            }
        }

        if (payments.isEmpty()) {
            item {
                Text(
                    "Sin pagos pendientes. Añade alquiler, servicios o suscripciones para no olvidarte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        items(payments, key = { it.id }) { payment ->
            PaymentRow(
                payment = payment,
                onPay = { onPay(payment) },
                onDelete = { onDeletePayment(payment) },
            )
        }
    }

    if (adding) {
        AddPaymentDialog(
            state = state,
            onConfirm = { name, amount, dueAt, recurrence, categoryId ->
                onAddPayment(name, amount, dueAt, recurrence, categoryId)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun PaymentRow(
    payment: ScheduledPaymentEntity,
    onPay: () -> Unit,
    onDelete: () -> Unit,
) {
    val today = java.time.LocalDate.now()
    val dueDate = Instant.ofEpochMilli(payment.dueAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val overdue = dueDate.isBefore(today)

    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = payment.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = dueDate.format(dayFormatter) +
                            if (overdue) " · vencido" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) Peligro else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MoneyText(
                    minor = payment.amountMinor,
                    currency = payment.currency,
                    style = MaterialTheme.typography.bodyLarge,
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Close, contentDescription = "Eliminar pago")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPay) { Text("Marcar pagado") }
                Text(
                    text = if (payment.recurrence == Recurrence.NONE) "único" else "mensual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = payment.currency,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ diálogos

@Composable
private fun EnvelopeDialog(
    category: CategoryEntity,
    current: EnvelopeProgress?,
    currency: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable {
        mutableStateOf(current?.limitMinor?.let { minorToInput(it) } ?: "")
    }
    val parsed = AmountParser.evaluateToMinor(text)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("${category.emoji} ${category.name}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Límite mensual en $currency",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("0,00") },
                    singleLine = true,
                    isError = text.isNotEmpty() && parsed == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (parsed != null) {
                    Text(
                        text = MoneyFormat.format(parsed, currency),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    if (current != null) {
                        TextButton(onClick = { onConfirm(0L) }) { Text("Quitar") }
                    }
                    TextButton(
                        onClick = { parsed?.let(onConfirm) },
                        enabled = parsed != null,
                    ) { Text("Guardar") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentDialog(
    state: HomeUiState,
    onConfirm: (String, Long, Long, Recurrence, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var recurrence by rememberSaveable { mutableStateOf(Recurrence.MONTHLY) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val defaultDue = remember {
        java.time.LocalDate.now().plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    var dueAt by rememberSaveable { mutableStateOf(defaultDue) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val amount = AmountParser.evaluateToMinor(amountText)
    val categories = state.categories.filter { it.kind == CategoryKind.EXPENSE }
    val dueDate = Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()).toLocalDate()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Nuevo pago", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nombre (alquiler, luz…)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Importe") },
                    singleLine = true,
                    isError = amountText.isNotEmpty() && amount == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Vence: ${dueDate.format(dayFormatter)}")
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = recurrence == Recurrence.MONTHLY,
                        onClick = { recurrence = Recurrence.MONTHLY },
                        label = { Text("Mensual") },
                    )
                    FilterChip(
                        selected = recurrence == Recurrence.NONE,
                        onClick = { recurrence = Recurrence.NONE },
                        label = { Text("Único") },
                    )
                }
                if (categories.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    CategoriesPicker(
                        categories = categories,
                        selected = categoryId,
                        onSelect = { categoryId = it },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    TextButton(
                        onClick = {
                            val parsedAmount = amount ?: return@TextButton
                            onConfirm(name, parsedAmount, dueAt, recurrence, categoryId)
                        },
                        enabled = name.isNotBlank() && amount != null,
                    ) { Text("Añadir") }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dueAt = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun CategoriesPicker(
    categories: List<CategoryEntity>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column {
        Text(
            "Categoría (opcional)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = category.id == selected,
                    onClick = { onSelect(if (category.id == selected) null else category.id) },
                    label = { Text("${category.emoji} ${category.name}") },
                )
            }
        }
    }
}

private fun minorToInput(minor: Long): String {
    val major = minor / 100
    val cents = (minor % 100).toInt()
    return if (cents == 0) major.toString() else "%d,%02d".format(major, cents)
}
