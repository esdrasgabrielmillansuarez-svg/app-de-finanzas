package com.suarezmillan.finanzas.core.report

import com.suarezmillan.finanzas.core.money.convertAmount
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

/**
 * Reporte del mes: resumen, movimientos y estado de los sobres.
 *
 * Es Kotlin puro (sin Android) para poder probarlo en la JVM. El PDF solo se
 * encarga de dibujar; todas las cifras salen de aquí.
 *
 * Reglas:
 *  - Los traspasos aparecen en la tabla pero **no** suman como ingreso o gasto.
 *  - Cada movimiento se convierte con su tasa histórica; si no hay tasa, se
 *    muestra en su divisa y no se suma (nunca se inventa una equivalencia).
 */
data class MonthlyReport(
    val month: YearMonth,
    val baseCurrency: String,
    val generatedAt: Long,
    val summary: ReportSummary,
    val rows: List<ReportRow>,
    val envelopes: List<ReportEnvelope>,
)

data class ReportSummary(
    val incomeMinor: Long,
    val expenseMinor: Long,
    val movementCount: Int,
    /** Movimientos del mes que quedaron fuera de los totales por falta de tasa. */
    val missingRateMovements: Int,
) {
    val balanceMinor: Long get() = incomeMinor - expenseMinor
}

data class ReportRow(
    val occurredAt: Long,
    val type: TxnType,
    val categoryName: String,
    val merchant: String?,
    /** Importe tal como se guardó (los gastos son negativos). */
    val amountMinor: Long,
    val currency: String,
    /** Importe absoluto convertido a la divisa base; `null` si falta la tasa. */
    val amountInBaseMinor: Long?,
) {
    val typeLabel: String
        get() = when (type) {
            TxnType.INCOME -> "Ingreso"
            TxnType.EXPENSE -> "Gasto"
            TxnType.TRANSFER -> "Traspaso"
        }
}

data class ReportEnvelope(
    val name: String,
    val emoji: String,
    val currency: String,
    val limitMinor: Long,
    val spentMinor: Long,
    val remainingMinor: Long,
    val windowLabel: String,
) {
    val isOver: Boolean get() = spentMinor > limitMinor
}

/**
 * Construye el reporte del mes indicado.
 *
 * [currentRate] se usa solo como respaldo cuando el movimiento no guardó tasa
 * histórica; si devuelve `null`, el importe queda sin convertir y se avisa.
 */
fun buildMonthlyReport(
    month: YearMonth,
    baseCurrency: String,
    txns: List<TxnEntity>,
    categoryName: (String?) -> String,
    envelopes: List<ReportEnvelope> = emptyList(),
    currentRate: (from: String, to: String) -> Double? = { _, _ -> null },
    zone: ZoneId = ZoneId.systemDefault(),
    generatedAt: Long = System.currentTimeMillis(),
): MonthlyReport {
    val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    val monthTxns = txns
        .filter { it.occurredAt >= start && it.occurredAt < end }
        .sortedBy { it.occurredAt }

    var income = 0L
    var expense = 0L
    var missing = 0

    val rows = monthTxns.map { txn ->
        val converted = convertAmount(
            amountMinor = abs(txn.amountMinor),
            from = txn.currency,
            to = baseCurrency,
            historicalRate = txn.fxRateToBase,
            historicalBase = txn.fxBaseCurrency,
            currentRate = currentRate,
        )
        when (txn.type) {
            TxnType.INCOME -> {
                if (converted == null) missing++ else income += converted
            }

            TxnType.EXPENSE -> {
                if (converted == null) missing++ else expense += converted
            }

            // Un traspaso mueve dinero entre cuentas propias: no es ingreso ni gasto.
            TxnType.TRANSFER -> Unit
        }
        ReportRow(
            occurredAt = txn.occurredAt,
            type = txn.type,
            categoryName = categoryName(txn.categoryId),
            merchant = txn.merchant?.takeIf { it.isNotBlank() },
            amountMinor = txn.amountMinor,
            currency = txn.currency,
            amountInBaseMinor = converted,
        )
    }

    return MonthlyReport(
        month = month,
        baseCurrency = baseCurrency,
        generatedAt = generatedAt,
        summary = ReportSummary(
            incomeMinor = income,
            expenseMinor = expense,
            movementCount = monthTxns.size,
            missingRateMovements = missing,
        ),
        rows = rows,
        envelopes = envelopes,
    )
}
