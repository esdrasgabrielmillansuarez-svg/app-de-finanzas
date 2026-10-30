package com.suarezmillan.finanzas.core.receipt

import java.time.LocalDate
import kotlin.math.roundToLong

/** Categoría sugerida a partir del texto de la factura. El mapeo a la BD lo hace la UI. */
enum class CategoryHint {
    SUPER,
    FOOD,
    TRANSPORT,
    HOME,
    FUN,
    HEALTH,
    BILLS,
}

/**
 * Palabras con las que se reconoce la pista en el nombre de una categoría del
 * usuario. Vive aquí (y no en la UI) para poder probarlo sin Android.
 */
val CategoryHint.keywords: List<String>
    get() = when (this) {
        CategoryHint.SUPER -> listOf("super", "mercado", "compras")
        CategoryHint.FOOD -> listOf("comida", "restaurante", "café", "cafe", "food")
        CategoryHint.TRANSPORT -> listOf("transporte", "gasolina", "auto", "taxi", "metro")
        CategoryHint.HOME -> listOf("casa", "hogar", "alquiler", "renta")
        CategoryHint.FUN -> listOf("ocio", "diversión", "diversion", "cine", "salida")
        CategoryHint.HEALTH -> listOf("salud", "farmacia", "médico", "medico")
        CategoryHint.BILLS -> listOf("servicio", "luz", "agua", "internet", "factura")
    }

fun CategoryHint.matches(categoryName: String): Boolean =
    keywords.any { categoryName.contains(it, ignoreCase = true) }

data class ReceiptAnalysis(
    val merchant: String?,
    val totalMinor: Long?,
    /** Divisa detectada en el texto (`USD`, `EUR`), o null si solo hay `$`. */
    val currency: String?,
    val date: LocalDate?,
    val taxId: String?,
    val itemsDetected: Int,
    /** 0..1. Menos de 0.6 → pedir confirmación explícita. */
    val confidence: Float,
    val categoryHint: CategoryHint?,
    val warnings: List<String>,
    val rawText: String,
) {
    val hasTotal: Boolean get() = totalMinor != null
}

/**
 * Extrae los datos de una factura a partir del texto OCR.
 *
 * Es **determinista y offline**: nada de modelos en la nube. Cualquier aparato
 * puede auditar por qué eligió un total. Si el OCR es basura, devuelve pocos
 * campos y `confidence` baja en vez de inventar.
 *
 * Solo trabaja con texto: el OCR vive en `ReceiptScanner` (ML Kit).
 */
object ReceiptParser {

    /** Importes con separador de miles o con dos decimales (nunca "2026" suelto). */
    private val AMOUNT = Regex("""\d{1,3}(?:[.\s]\d{3})+(?:[.,]\d{2})?|\d+[.,]\d{2}""")

    private val TOTAL_KEYS = listOf(
        "TOTAL A PAGAR",
        "TOTAL FINAL",
        "IMPORTE TOTAL",
        "TOTAL COMPRA",
        "A PAGAR",
        "TOTAL",
        "SALDO",
        "MONTO",
    )

    private val NUMERIC_DATE = Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})""")
    private val TEXT_DATE = Regex(
        """(\d{1,2})\s*[/\-\s]\s*(ENE|FEB|MAR|ABR|MAY|JUN|JUL|AGO|SEP|SET|OCT|NOV|DIC)[A-Za-z]*\.?\s*[/\-\s]?\s*(\d{2,4})""",
    )
    private val MONTHS = mapOf(
        "ENE" to 1, "FEB" to 2, "MAR" to 3, "ABR" to 4, "MAY" to 5, "JUN" to 6,
        "JUL" to 7, "AGO" to 8, "SEP" to 9, "SET" to 9, "OCT" to 10, "NOV" to 11, "DIC" to 12,
    )

    private val TAX_ID = Regex(
        """(?:CUIT|C\.U\.I\.T\.?|RUC|RUT|NIT|RFC)[:\s.#\-]*(\d[\d.\-]{5,18}\d)""",
    )

    private val ITEM_EXCLUDE = listOf("SUBTOTAL", "IVA", "CUIT", "RUC", "RUT", "NIT", "RFC", "FECHA", "TOTAL", "SALDO", "MONTO")

    private val MERCHANT_STOPWORDS = listOf(
        "FACTURA", "TICKET", "RECIBO", "COMPROBANTE", "FECHA", "HORA", "CAJA", "CAJERO",
        "VENDEDOR", "TEL", "TELEFONO", "WWW", "HTTP", "DOMICILIO", "DIRECCION", "AVENIDA",
        "CALLE", "IVA", "CUIT", "RUC", "RUT", "NIT", "RFC", "SUBTOTAL", "TOTAL",
    )

    fun parse(
        text: String,
        defaultCurrency: String = "ARS",
    ): ReceiptAnalysis {
        val lines = text.lines()
            .map { it.replace('\u00A0', ' ').trim() }
            .filter { it.isNotEmpty() }
            .map { it.replace(Regex("""\s{2,}"""), " ") }

        val upper = lines.joinToString("\n").uppercase()

        val totalMinor = findTotal(lines)
        val date = findDate(lines)
        val merchant = findMerchant(lines)
        val currency = detectCurrency(upper) ?: defaultCurrency.takeIf { upper.contains("$") }
        val taxId = TAX_ID.find(upper)?.groupValues?.getOrNull(1)?.trim()
        val items = countItems(lines)
        val hint = hintFor(upper)

        val warnings = buildList {
            if (totalMinor == null) add("No se detectó el total")
            if (date == null) add("No se detectó la fecha")
            if (merchant == null) add("No se detectó el comercio")
            if (totalMinor != null && totalMinor <= 0L) add("Importe inválido")
        }

        var confidence = 0.25f
        if (totalMinor != null) confidence += 0.40f
        if (date != null) confidence += 0.20f
        if (merchant != null) confidence += 0.15f
        if (confidence > 0.95f) confidence = 0.95f

        return ReceiptAnalysis(
            merchant = merchant,
            totalMinor = totalMinor,
            currency = currency,
            date = date,
            taxId = taxId,
            itemsDetected = items,
            confidence = confidence,
            categoryHint = hint,
            warnings = warnings,
            rawText = text,
        )
    }

    // --------------------------------------------------------------- total

    private fun findTotal(lines: List<String>): Long? {
        for (key in TOTAL_KEYS) {
            lines.forEachIndexed { index, line ->
                val matches = line.contains(key) &&
                    !(key == "TOTAL" && line.contains("SUBTOTAL")) &&
                    !(key == "TOTAL" && line.contains("TOTAL SIN"))
                if (!matches) return@forEachIndexed

                val sameLine = amounts(line)
                if (sameLine.isNotEmpty()) return sameLine.last()

                // Muchas facturas imprimen "TOTAL" y el importe en la línea siguiente.
                val next = lines.getOrNull(index + 1)?.let { amounts(it).lastOrNull() }
                if (next != null) return next
            }
        }
        // Fallback: el importe más grande del ticket (suele ser el total).
        return lines
            .flatMap { amounts(it) }
            .filter { it in 100L..10_000_000_00L }
            .maxOrNull()
    }

    private fun amounts(line: String): List<Long> =
        AMOUNT.findAll(line).mapNotNull { parseAmount(it.value) }.toList()

    /** Líneas con importe que no son totales, impuestos ni identificadores. */
    private fun countItems(lines: List<String>): Int = lines.count { line ->
        val upper = line.uppercase()
        AMOUNT.containsMatchIn(line) &&
            ITEM_EXCLUDE.none { upper.contains(it) } &&
            !NUMERIC_DATE.containsMatchIn(line) &&
            !TEXT_DATE.containsMatchIn(upper)
    }

    private fun parseAmount(token: String): Long? {
        val compact = token.replace(" ", "")
        val normalized = when {
            compact.contains('.') && compact.contains(',') ->
                compact.replace(".", "").replace(',', '.')
            compact.contains(',') -> {
                val decimals = compact.substringAfterLast(',').length
                if (decimals == 3) compact.replace(",", "") else compact.replace(',', '.')
            }
            compact.contains('.') -> {
                val decimals = compact.substringAfterLast('.').length
                if (decimals == 3) compact.replace(".", "") else compact
            }
            else -> compact
        }
        return normalized.toDoubleOrNull()?.let { (it * 100).roundToLong() }
    }

    // --------------------------------------------------------------- fecha

    private fun findDate(lines: List<String>): LocalDate? {
        for (line in lines) {
            NUMERIC_DATE.find(line)?.let { match ->
                val (day, month, year) = match.destructured
                parseDate(day.toIntOrNull(), month.toIntOrNull(), year.toIntOrNull())
                    ?.let { return it }
            }
            TEXT_DATE.find(line.uppercase())?.let { match ->
                val (day, monthName, year) = match.destructured
                val month = MONTHS[monthName] ?: return@let
                parseDate(day.toIntOrNull(), month, year.toIntOrNull())?.let { return it }
            }
        }
        return null
    }

    private fun parseDate(day: Int?, month: Int?, year: Int?): LocalDate? {
        if (day == null || month == null || year == null) return null
        if (day !in 1..31 || month !in 1..12) return null
        val fullYear = if (year < 100) 2000 + year else year
        if (fullYear !in 2000..2100) return null
        return runCatching { LocalDate.of(fullYear, month, day) }.getOrNull()
            ?: runCatching { LocalDate.of(fullYear, day, month) }.getOrNull()
    }

    // --------------------------------------------------------------- comercio

    private fun findMerchant(lines: List<String>): String? {
        for (line in lines.take(12)) {
            val upper = line.uppercase()
            if (upper in MERCHANT_STOPWORDS) continue
            if (MERCHANT_STOPWORDS.any { upper.startsWith(it) }) continue
            val letters = line.count { it.isLetter() }
            if (letters < 3) continue
            if (line.first().isDigit()) continue
            if (AMOUNT.containsMatchIn(line)) continue
            val cleaned = line
                .replace(Regex("""[^\p{L}\p{N} .,&'\-]"""), "")
                .trim()
                .take(48)
            if (cleaned.count { it.isLetter() } < 3) continue
            return cleaned
        }
        return null
    }

    // --------------------------------------------------------------- divisa / categoría

    private fun detectCurrency(upper: String): String? = when {
        upper.contains("US\$") || upper.contains("U\$S") || Regex("""\bUSD\b""").containsMatchIn(upper) -> "USD"
        upper.contains("€") || Regex("""\bEUR\b""").containsMatchIn(upper) -> "EUR"
        else -> null
    }

    private fun hintFor(upper: String): CategoryHint? {
        val rules = listOf(
            CategoryHint.BILLS to listOf(
                "EDESUR", "EDENOR", "AYSA", "GAS NATURAL", "MOVISTAR", "CLARO", "PERSONAL",
                "TELECOM", "FIBERTEL", "INTERNET", "TELEFON", "CABLEVISION", "DIRECTV",
            ),
            CategoryHint.SUPER to listOf(
                "SUPERMERCADO", "SUPER", "MERCADO", "ALMACEN", "COTO", "CARREFOUR", "JUMBO",
                "WALMART", "DIA", "EXITO", "LIDER", "SORIANA", "ANONIMA", "MAKRO",
            ),
            CategoryHint.HEALTH to listOf(
                "FARMACIA", "DROGUERIA", "FARMACITY", "HOSPITAL", "CLINICA", "LABORATORIO", "OPTICA",
            ),
            CategoryHint.TRANSPORT to listOf(
                "COMBUSTIBLE", "NAFTA", "GASOIL", "YPF", "SHELL", "AXION", "PETROBRAS",
                "UBER", "CABIFY", "TAXI", "SUBE", "PEAJE",
            ),
            CategoryHint.FOOD to listOf(
                "RESTAURANT", "CAFE", "CAFETERIA", "PIZZERIA", "HELADERIA", "BURGER",
                "SUSHI", "PANADERIA", "BAR", "ROTISERIA",
            ),
            CategoryHint.HOME to listOf(
                "FERRETERIA", "MUEBLERIA", "BAZAR", "IKEA", "SODIMAC", "EASY", "HOGAR",
            ),
            CategoryHint.FUN to listOf(
                "CINE", "TEATRO", "SPOTIFY", "NETFLIX", "STEAM", "PLAYSTATION", "GIMNASIO",
            ),
        )
        // Puntúa cada categoría y gana la que más señales tenga. Así "NAFTA SUPER"
        // cae en TRANSPORT (2 aciertos) y no en SUPER (1), sin depender del orden.
        var best: CategoryHint? = null
        var bestScore = 0
        for ((hint, words) in rules) {
            val score = words.count { word ->
                val pattern = if (word.length <= 3) {
                    Regex("""\b${Regex.escape(word)}\b""")
                } else {
                    Regex(Regex.escape(word))
                }
                pattern.containsMatchIn(upper)
            }
            if (score > bestScore) {
                best = hint
                bestScore = score
            }
        }
        return best
    }
}
