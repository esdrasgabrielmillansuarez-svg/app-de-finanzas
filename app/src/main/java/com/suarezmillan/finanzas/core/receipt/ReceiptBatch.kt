package com.suarezmillan.finanzas.core.receipt

import com.suarezmillan.finanzas.core.money.MoneyFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** De dónde salió la foto: define si hay que borrar el archivo temporal al descartar. */
enum class ReceiptSource { CAMERA, GALLERY }

enum class ReceiptStatus {
    /** En cola o leyéndose con ML Kit. */
    SCANNING,

    /** Leída. Puede faltar el total o la categoría: eso lo dice [ReceiptDraft.needsReview]. */
    READY,

    /** ML Kit no pudo con la imagen. */
    FAILED,
}

/** Categoría de gasto que el usuario ya tiene, para sugerir la del OCR. */
data class ReceiptCategory(val id: String, val name: String)

/**
 * Un importe del desglose de una factura (una línea del ticket).
 *
 * [id] es estable para que la UI conserve el texto que se está tecleando aunque
 * se quite otro importe de la lista. [label] es el concepto leído por el OCR
 * (`LECHE ENTERA 1L`) y se puede corregir a mano.
 */
data class ReceiptPart(
    val id: String,
    val amountMinor: Long = 0L,
    val label: String = "",
)

/**
 * Un comprobante dentro de la bandeja de carga masiva.
 *
 * El OCR **propone** importe, comercio, divisa y categoría; el usuario confirma
 * antes de guardar. Nunca se crea un movimiento solo.
 *
 * [imageUri] se guarda como texto para que todo este archivo sea Kotlin puro y
 * se pueda probar en la JVM sin Android.
 */
data class ReceiptDraft(
    val id: String,
    val source: ReceiptSource,
    val imageUri: String,
    val status: ReceiptStatus = ReceiptStatus.SCANNING,
    val amountMinor: Long? = null,
    val currency: String = "",
    val merchant: String? = null,
    val categoryId: String? = null,
    val occurredAt: Long = 0L,
    /**
     * Desglose opcional de la factura. Si está vacío se usa [amountMinor]; si
     * tiene líneas, el total es su suma. Así una factura con varios conceptos
     * se guarda como **un solo gasto** con el total correcto.
     */
    val parts: List<ReceiptPart> = emptyList(),
    /** Todos los campos leídos del comprobante (operación, banco, origen...). */
    val details: List<ReceiptDetail> = emptyList(),
    /** Datos estructurados si el comprobante es un PagoMóvil. */
    val pagoMovil: PagoMovilData? = null,
    /** Subtotal, impuestos y descuento tal como los imprimió la factura. */
    val subtotalMinor: Long? = null,
    val taxMinor: Long? = null,
    val discountMinor: Long? = null,
    /** El usuario lo quiere incluir en el guardado masivo. */
    val include: Boolean = true,
    val confidence: Float = 0f,
    val warnings: List<String> = emptyList(),
    val error: String? = null,
) {
    /** Total de la factura: la suma del desglose o el importe leído por el OCR. */
    val totalMinor: Long?
        get() = if (parts.isEmpty()) {
            amountMinor
        } else {
            parts.sumOf { it.amountMinor }.takeIf { it > 0L }
        }

    /** Datos mínimos para crear el gasto: total positivo y categoría. */
    val hasRequiredData: Boolean
        get() = status == ReceiptStatus.READY &&
            (totalMinor ?: 0L) > 0L &&
            categoryId != null

    /** Comprobante de PagoMóvil: un solo pago en bolívares, sin desglose. */
    val isPagoMovil: Boolean get() = pagoMovil != null

    /** Lo que realmente se guardará al confirmar la bandeja. */
    val willSave: Boolean get() = hasRequiredData && include

    /** Leída pero incompleta: hay que corregirla a mano antes de guardar. */
    val needsReview: Boolean get() = status == ReceiptStatus.READY && !hasRequiredData

    /** La foto se puede borrar del caché cuando el borrador ya no se usa. */
    val ownsTemporaryFile: Boolean get() = source == ReceiptSource.CAMERA
}

/** Borrador recién creado, antes de que ML Kit lo lea. */
fun newReceiptDraft(
    id: String,
    source: ReceiptSource,
    imageUri: String,
    baseCurrency: String,
    now: Long,
): ReceiptDraft = ReceiptDraft(
    id = id,
    source = source,
    imageUri = imageUri,
    currency = baseCurrency,
    occurredAt = now,
)

/**
 * Divisa del borrador: la detectada solo si es una de las dos divisas de la app.
 * Cualquier otra (o `null`) cae en la divisa base, sin inventar tasas.
 */
fun receiptCurrency(detected: String?, baseCurrency: String, secondaryCurrency: String): String {
    val clean = detected?.trim()?.uppercase().orEmpty()
    return when (clean) {
        baseCurrency.uppercase() -> baseCurrency
        secondaryCurrency.uppercase() -> secondaryCurrency
        else -> baseCurrency
    }
}

/**
 * Fecha del gasto: la de la factura si el OCR la leyó y no está en el futuro.
 * Si falta o es futura (suele ser un vencimiento), se usa [now].
 */
fun receiptOccurredAt(date: LocalDate?, zone: ZoneId, now: Long): Long {
    if (date == null) return now
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    if (date.isAfter(today)) return now
    return date.atStartOfDay(zone).toInstant().toEpochMilli()
}

/** Completa el borrador con lo que leyó ML Kit y resuelve la categoría sugerida. */
fun ReceiptDraft.withAnalysis(
    analysis: ReceiptAnalysis,
    baseCurrency: String,
    secondaryCurrency: String,
    categories: List<ReceiptCategory>,
    now: Long,
    zone: ZoneId,
): ReceiptDraft {
    val parts = detailedParts(analysis)
    val mismatch = analysis.items.isNotEmpty() && parts.isEmpty()
    val pagoMovil = analysis.pagoMovil
    return copy(
        status = ReceiptStatus.READY,
        amountMinor = analysis.totalMinor,
        // Si el OCR leyó el desglose completo, se carga entero; si no cuadra con
        // el total, se deja el importe único para que el usuario decida.
        parts = parts,
        // Un PagoMóvil siempre está en bolívares; el resto, en una de las dos
        // divisas de la app.
        currency = if (pagoMovil != null) {
            PAGO_MOVIL_CURRENCY
        } else {
            receiptCurrency(analysis.currency, baseCurrency, secondaryCurrency)
        },
        merchant = (pagoMovil?.merchantLabel() ?: analysis.merchant)
            ?.trim()
            ?.take(40)
            ?.takeIf { it.isNotBlank() },
        categoryId = suggestCategory(analysis.categoryHint, categories),
        occurredAt = receiptOccurredAt(analysis.date, zone, now),
        include = true,
        confidence = analysis.confidence,
        warnings = if (mismatch) {
            analysis.warnings + "El desglose leído no cuadra con el total: revísalo"
        } else {
            analysis.warnings
        },
        error = null,
        details = pagoMovil?.fields()?.takeIf { it.isNotEmpty() } ?: analysis.details,
        pagoMovil = pagoMovil,
        subtotalMinor = analysis.subtotalMinor,
        taxMinor = analysis.taxMinor,
        discountMinor = analysis.discountMinor,
    )
}

/**
 * Desglose completo leído por el OCR. Solo se usa cuando la suma cuadra con el
 * total: un desglose que no cuadra guardaría un gasto con el importe equivocado.
 *
 * Si los conceptos suman el subtotal, se añaden impuestos y descuento para
 * llegar al total impreso.
 */
fun detailedParts(analysis: ReceiptAnalysis): List<ReceiptPart> {
    val total = analysis.totalMinor ?: return emptyList()
    val items = analysis.items.take(MAX_RECEIPT_PARTS)
    if (items.isEmpty()) return emptyList()

    val base = items.map { item ->
        ReceiptPart(UUID.randomUUID().toString(), item.amountMinor, item.label.take(40))
    }
    if (base.sumOf { it.amountMinor } == total) return base

    val withTax = analysis.taxMinor?.takeIf { it > 0L }?.let { tax ->
        base + ReceiptPart(UUID.randomUUID().toString(), tax, "Impuestos")
    } ?: base
    if (withTax.sumOf { it.amountMinor } == total) return withTax

    val withDiscount = analysis.discountMinor?.takeIf { it > 0L }?.let { discount ->
        withTax + ReceiptPart(UUID.randomUUID().toString(), -discount, "Descuento")
    } ?: withTax
    if (withDiscount.sumOf { it.amountMinor } == total) return withDiscount

    return emptyList()
}

/** Primera categoría del usuario (en su orden) cuyo nombre encaja con la pista del OCR. */
fun suggestCategory(hint: CategoryHint?, categories: List<ReceiptCategory>): String? {
    val wanted = hint ?: return null
    return categories.firstOrNull { wanted.matches(it.name) }?.id
}

/** Tope del desglose: un ticket real no tiene cientos de líneas. */
const val MAX_RECEIPT_PARTS = 20

/**
 * Activa el desglose conservando el total leído por el OCR como primera línea.
 * Si no había total, empieza con una línea vacía.
 */
fun ReceiptDraft.withBreakdown(): ReceiptDraft =
    if (parts.isNotEmpty()) {
        this
    } else {
        copy(parts = listOf(ReceiptPart(UUID.randomUUID().toString(), amountMinor ?: 0L)))
    }

/** Añade una línea vacía al desglose (hasta [MAX_RECEIPT_PARTS]). */
fun ReceiptDraft.withPartAdded(): ReceiptDraft =
    if (parts.size >= MAX_RECEIPT_PARTS) {
        this
    } else {
        copy(parts = parts + ReceiptPart(UUID.randomUUID().toString()))
    }

/** Cambia el importe de una línea. Un valor nulo o negativo cuenta como cero. */
fun ReceiptDraft.withPartAmount(partId: String, amountMinor: Long?): ReceiptDraft = copy(
    parts = parts.map { part ->
        if (part.id == partId) part.copy(amountMinor = amountMinor?.coerceAtLeast(0L) ?: 0L) else part
    },
)

/** Corrige el concepto de una línea del desglose. */
fun ReceiptDraft.withPartLabel(partId: String, label: String): ReceiptDraft = copy(
    parts = parts.map { part ->
        if (part.id == partId) part.copy(label = label.take(40)) else part
    },
)

/**
 * Detalle legible para guardarlo como nota del movimiento: todos los campos del
 * comprobante y, si hay, el desglose con conceptos.
 *
 * Un PagoMóvil se guarda con un campo por línea, para poder reconstruir la
 * operación (número, cédula, origen, destino y banco) desde el detalle del gasto.
 * Si el usuario parte el monto, el desglose se añade al final.
 */
fun ReceiptDraft.detailsNote(): String? {
    val header = pagoMovil?.note()
    val fields = details.joinToString(" · ") { "${it.label}: ${it.value}" }
    val lines = parts
        .filter { it.label.isNotBlank() }
        .joinToString(" · ") { "${it.label}: ${MoneyFormat.format(it.amountMinor, currency)}" }
    return buildString {
        when {
            header != null -> append(header)
            fields.isNotBlank() -> append(fields)
        }
        if (lines.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append("Desglose: ").append(lines)
        }
    }.take(900).takeIf { it.isNotBlank() }
}

/** Quita una línea del desglose. Sin líneas, vuelve a mandar [amountMinor]. */
fun ReceiptDraft.withoutPart(partId: String): ReceiptDraft =
    copy(parts = parts.filterNot { it.id == partId })

/** Borradores que se guardarán al pulsar "Guardar N gastos". */
fun draftsToSave(drafts: List<ReceiptDraft>): List<ReceiptDraft> = drafts.filter { it.willSave }

/** Cuántas fotos siguen leyéndose (para el indicador de progreso). */
fun draftsScanning(drafts: List<ReceiptDraft>): Int =
    drafts.count { it.status == ReceiptStatus.SCANNING }
