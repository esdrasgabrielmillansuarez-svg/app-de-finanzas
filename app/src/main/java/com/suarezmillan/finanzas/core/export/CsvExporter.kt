package com.suarezmillan.finanzas.core.export

import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Exportación CSV de movimientos.
 *
 * - Importes como decimal con punto (`-1250.00`), listos para Excel/Sheets.
 * - Se exporta **todo** lo que la app sabe de cada fila, incluidas las divisas.
 * - Separador `,` y comillas dobles escapadas según RFC 4180.
 */
object CsvExporter {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun build(
        txns: List<TxnEntity>,
        categoryName: (String?) -> String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val header = listOf(
            "fecha", "tipo", "importe", "divisa", "categoria", "comercio", "nota", "tasa_a_base",
        ).joinToString(",")

        val rows = txns.joinToString("\n") { txn ->
            listOf(
                dateFormat.format(Instant.ofEpochMilli(txn.occurredAt).atZone(zone)),
                txn.type.name.lowercase(),
                String.format(Locale.US, "%.2f", txn.amountMinor / 100.0),
                txn.currency,
                categoryName(txn.categoryId),
                txn.merchant.orEmpty(),
                txn.note.orEmpty(),
                txn.fxRateToBase?.let { String.format(Locale.US, "%.6f", it) } ?: "",
            ).joinToString(",") { escape(it) }
        }

        return if (rows.isEmpty()) header else "$header\n$rows"
    }

    /** RFC 4180: si hay coma, comilla o salto de línea, se encierra y se duplican comillas. */
    fun escape(field: String): String {
        val needsQuotes = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = field.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
