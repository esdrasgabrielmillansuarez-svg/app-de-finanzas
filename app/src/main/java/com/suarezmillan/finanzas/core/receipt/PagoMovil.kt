package com.suarezmillan.finanzas.core.receipt

import com.suarezmillan.finanzas.core.money.MoneyFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Un PagoMóvil siempre se mueve en bolívares. */
const val PAGO_MOVIL_CURRENCY = "VES"

private val PAGO_MOVIL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Datos de un comprobante de **PagoMóvil**.
 *
 * No es una factura de compra: no trae líneas de concepto, sino los datos de la
 * operación (monto, fecha, número de operación, cédula, cuenta origen, teléfono
 * destino y banco). Modelarlo aparte permite mostrar cada campo con su nombre
 * real y guardarlos completos en la nota del movimiento.
 *
 * Todos los campos son opcionales: el OCR puede leer unos y fallar en otros, y
 * conviene conservar lo que sí leyó en vez de descartar el comprobante entero.
 */
data class PagoMovilData(
    val amountMinor: Long? = null,
    val date: LocalDate? = null,
    val operation: String? = null,
    val idNumber: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val bankCode: String? = null,
    val bankName: String? = null,
    val concept: String? = null,
    val holder: String? = null,
) {
    /** Banco como lo imprime el comprobante: `0105 - BANCO MERCANTIL`. */
    val bank: String?
        get() = when {
            bankCode != null && bankName != null -> "$bankCode - $bankName"
            else -> bankName ?: bankCode
        }

    /**
     * Nombre para el campo "Comercio" del movimiento. El pagador reconoce antes
     * al destinatario que al banco, así que se prioriza titular, concepto y
     * teléfono destino; el banco queda como último recurso.
     */
    fun merchantLabel(): String? = when {
        !holder.isNullOrBlank() -> holder
        !concept.isNullOrBlank() -> concept
        !destination.isNullOrBlank() -> "PagoMóvil $destination"
        !bankName.isNullOrBlank() -> bankName
        else -> null
    }?.trim()?.take(40)?.takeIf { it.isNotEmpty() }

    /** Todos los campos presentes, en el orden en que se leen en el comprobante. */
    fun fields(): List<ReceiptDetail> = buildList {
        amountMinor?.let {
            add(ReceiptDetail("Monto", MoneyFormat.format(it, PAGO_MOVIL_CURRENCY)))
        }
        date?.let { add(ReceiptDetail("Fecha", it.format(PAGO_MOVIL_DATE))) }
        operation?.let { add(ReceiptDetail("Operación", it)) }
        idNumber?.let { add(ReceiptDetail("Identificación", it)) }
        origin?.let { add(ReceiptDetail("Origen", it)) }
        destination?.let { add(ReceiptDetail("Destino", it)) }
        bank?.let { add(ReceiptDetail("Banco", it)) }
        concept?.let { add(ReceiptDetail("Concepto", it)) }
        holder?.let { add(ReceiptDetail("Titular", it)) }
    }

    /**
     * Nota del movimiento: un campo por línea para que el comprobante se pueda
     * reconstruir después desde el detalle del gasto.
     */
    fun note(): String? = fields()
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "\n", prefix = "PagoMóvil\n") { "${it.label}: ${it.value}" }
}
