package com.suarezmillan.finanzas.core.receipt

import java.time.LocalDate
import java.time.ZoneId

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
 * Una factura dentro de la bandeja de carga masiva.
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
    /** El usuario lo quiere incluir en el guardado masivo. */
    val include: Boolean = true,
    val confidence: Float = 0f,
    val warnings: List<String> = emptyList(),
    val error: String? = null,
) {
    /** Datos mínimos para crear el gasto: importe positivo y categoría. */
    val hasRequiredData: Boolean
        get() = status == ReceiptStatus.READY &&
            amountMinor != null &&
            amountMinor > 0L &&
            categoryId != null

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
): ReceiptDraft = copy(
    status = ReceiptStatus.READY,
    amountMinor = analysis.totalMinor,
    currency = receiptCurrency(analysis.currency, baseCurrency, secondaryCurrency),
    merchant = analysis.merchant?.trim()?.take(40)?.takeIf { it.isNotBlank() },
    categoryId = suggestCategory(analysis.categoryHint, categories),
    occurredAt = receiptOccurredAt(analysis.date, zone, now),
    include = true,
    confidence = analysis.confidence,
    warnings = analysis.warnings,
    error = null,
)

/** Primera categoría del usuario (en su orden) cuyo nombre encaja con la pista del OCR. */
fun suggestCategory(hint: CategoryHint?, categories: List<ReceiptCategory>): String? {
    val wanted = hint ?: return null
    return categories.firstOrNull { wanted.matches(it.name) }?.id
}

/** Borradores que se guardarán al pulsar "Guardar N gastos". */
fun draftsToSave(drafts: List<ReceiptDraft>): List<ReceiptDraft> = drafts.filter { it.willSave }

/** Cuántas fotos siguen leyéndose (para el indicador de progreso). */
fun draftsScanning(drafts: List<ReceiptDraft>): Int =
    drafts.count { it.status == ReceiptStatus.SCANNING }
