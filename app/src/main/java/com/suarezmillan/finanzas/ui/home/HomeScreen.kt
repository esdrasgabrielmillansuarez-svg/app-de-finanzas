package com.suarezmillan.finanzas.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.core.coach.CoachAction
import com.suarezmillan.finanzas.core.coach.CoachMessage
import com.suarezmillan.finanzas.core.money.MoneyFormat
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.ui.HomeUiState
import com.suarezmillan.finanzas.ui.components.EquivalenceText
import com.suarezmillan.finanzas.ui.components.EquivalentRateNote
import com.suarezmillan.finanzas.ui.components.MoneyText
import com.suarezmillan.finanzas.ui.theme.currencyAccent
import java.time.format.DateTimeFormatter
import java.util.Locale

private val txnDayFormat = DateTimeFormatter.ofPattern("d MMM", Locale("es", "AR"))

@Composable
fun HomeScreen(
    state: HomeUiState,
    userName: String,
    coach: CoachMessage,
    onAnotherCoach: () -> Unit,
    onCoachAction: (CoachAction) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenCurrencies: () -> Unit,
    onOpenCalculator: () -> Unit,
    onTxnClick: (TxnEntity) -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val monthName = remember {
        java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("MMMM", java.util.Locale("es", "AR")))
            .replaceFirstChar { it.uppercase() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(monthName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Hola, ${userName.trim().substringBefore(' ')}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        // 0. Sticker contextual (cambia según vencimientos, sobres y hábito)
        item {
            StickerCard(
                message = coach,
                onAnother = onAnotherCoach,
                onAction = onCoachAction,
            )
        }

        // 1. Saldo consolidado
        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyDot(state.baseCurrency, dark)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Saldo en ${state.baseCurrency}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MoneyText(
                        minor = state.balanceMinor,
                        currency = state.baseCurrency,
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyDot(state.secondaryCurrency, dark)
                        Spacer(Modifier.size(8.dp))
                        MoneyText(
                            minor = state.secondaryBalanceMinor,
                            currency = state.secondaryCurrency,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        onClick = onOpenCurrencies,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "1 ${state.secondaryCurrency} = " +
                                "${com.suarezmillan.finanzas.core.money.MoneyFormat.formatRate(state.rate)} " +
                                state.baseCurrency +
                                if (state.rateIsManual) " · fija" else "",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }

        // 2. Calculadora de cambio: consulta rápida, sin crear movimientos.
        item {
            Card(
                onClick = onOpenCalculator,
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Calculadora de cambio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            state.usdToVesRate?.let {
                                "Dólares y bolívares · 1 USD = ${MoneyFormat.formatRate(it)} VES"
                            } ?: "Dólares y bolívares con la tasa activa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 3. Te queda este mes = ingresos reales - gastos del mes
        item {
            Card(
                onClick = onOpenPlan,
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Te queda este mes",
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
                                    "Sin ingresos ni gastos este mes",
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
                    Spacer(Modifier.height(10.dp))
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
                        if (state.budgetLimitMinor > 0L) {
                            Text(
                                "Límite de sobres: ${MoneyFormat.format(state.budgetLimitMinor, state.baseCurrency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        EquivalentRateNote(
                            usdToVesRate = state.usdToVesRate,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        if (state.missingRateExpenses > 0) {
                            Text(
                                "${state.missingRateExpenses} gasto(s) del mes sin cotización no se sumaron.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    } else {
                        Text(
                            if (state.spentMinor > 0L) {
                                "Registra un ingreso para calcular tu disponible del mes."
                            } else {
                                "Registra un ingreso o un gasto para empezar."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.budgetLimitMinor > 0L) {
                            Text(
                                "Límite de sobres: ${MoneyFormat.format(state.budgetLimitMinor, state.baseCurrency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // 3. Proyección
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Proyección al cierre", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(
                        minor = state.projectedCloseMinor,
                        currency = state.baseCurrency,
                        approximate = true,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "estimado con tu ritmo de gasto actual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Text("Últimos movimientos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.recent.isEmpty()) {
            item {
                Text(
                    "Aún no hay movimientos. Toca + para registrar el primero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(state.recent, key = { it.id }) { txn ->
            TxnRow(txn = txn, onClick = { onTxnClick(txn) })
        }
    }
}

@Composable
fun TxnRow(txn: TxnEntity, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("💸")
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = txn.merchant ?: txn.note ?: "Movimiento",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = buildString {
                    append(txnDayFormat.format(java.time.Instant.ofEpochMilli(txn.occurredAt).atZone(java.time.ZoneId.systemDefault())))
                    append(" · ")
                    append(txn.currency)
                    append(" · ")
                    append(
                        when (txn.type.name) {
                            "EXPENSE" -> "gasto"
                            "INCOME" -> "ingreso"
                            else -> "transferencia"
                        },
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MoneyText(
            minor = txn.amountMinor,
            currency = txn.currency,
            signed = true,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun CurrencyDot(currency: String, dark: Boolean) {
    val color = currencyAccent(currency, dark)
    Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
}

/** Heurística local: suficiente para elegir el acento de divisa sin tocar el sistema. */
private fun androidx.compose.ui.graphics.Color.luminanceIsDark(): Boolean {
    val l = 0.299f * red + 0.587f * green + 0.114f * blue
    return l < 0.5f
}
