package com.suarezmillan.finanzas.core.money

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Importe monetario.
 *
 * Regla del proyecto: **el dinero nunca se guarda en `Double`**. Se guarda en
 * unidades menores (`minor`, centavos) junto al código ISO de su divisa.
 *
 * `minor = 12_450_00` → `$ 12.450,00`
 */
data class Money(
    val minor: Long,
    val currency: String,
) {
    val isZero: Boolean get() = minor == 0L
    val isNegative: Boolean get() = minor < 0L

    operator fun plus(other: Money): Money {
        require(other.currency == currency) {
            "No se suman divisas distintas ($currency + ${other.currency}); convierte antes."
        }
        return Money(minor + other.minor, currency)
    }

    operator fun minus(other: Money): Money = plus(Money(-other.minor, other.currency))

    operator fun times(factor: Int): Money = Money(minor * factor, currency)

    companion object {
        /** 12.45 → 1245 (redondeo bancario de centavos). */
        fun ofMajor(value: Double, currency: String): Money =
            Money(Math.round(value * 100.0), currency)
    }
}

/** Formato de presentación. No se usa para cálculo. */
object MoneyFormat {

    private val locale = Locale("es", "AR")

    private val symbols = mapOf(
        "ARS" to "$",
        "COP" to "$",
        "CLP" to "$",
        "MXN" to "$",
        "PEN" to "S/",
        "UYU" to "\$U",
        "VES" to "Bs.",
        "BRL" to "R$",
        "EUR" to "€",
        "USD" to "US$",
    )

    fun symbol(currency: String): String = symbols[currency] ?: currency

    /** `$ 12.450,00` — sin signo, con cifras tabulares en la UI. */
    fun format(minor: Long, currency: String): String {
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = true
        }
        return "${symbol(currency)} ${nf.format(abs(minor) / 100.0)}"
    }

    /** `− $ 12.450,00` / `+ US$ 250,00` — el signo se muestra aparte del símbolo. */
    fun formatSigned(minor: Long, currency: String): String {
        val sign = if (minor < 0) "−" else "+"
        return "$sign ${format(minor, currency)}"
    }

    /** `≈ $ 152.000` para agregados convertidos (nunca sin el ≈). */
    fun formatApprox(minor: Long, currency: String): String {
        val nf = NumberFormat.getIntegerInstance(locale)
        return "≈ ${symbol(currency)} ${nf.format(abs(minor) / 100.0)}"
    }

    /**
     * Tasa de cambio legible: `1.050` · `3,75` · `0,000952`.
     * Sin ceros de relleno: la magnitud manda cuántos decimales mostrar.
     */
    fun formatRate(value: Double, maxDecimals: Int = 6): String {
        if (!value.isFinite() || value <= 0.0) return "—"
        val decimals = when {
            value >= 1.0 -> 2
            value >= 0.01 -> 4
            else -> maxDecimals
        }
        val df = DecimalFormat("#,##0." + "#".repeat(decimals), DecimalFormatSymbols(locale))
        return df.format(value)
    }
}
