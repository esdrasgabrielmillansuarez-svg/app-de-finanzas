package com.suarezmillan.finanzas.core.budget

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Cadencia de un sobre.
 *
 * Los periodos se calculan por calendario, no por "hoy + 30 días":
 *  - [MONTHLY]: día 1 al último día del mes.
 *  - [BIWEEKLY]: 1–15 y 16 al último día del mes.
 *  - [WEEKLY]: lunes a domingo.
 *
 * Así el sobre se cierra solo y el arrastre siempre parte de fechas reales.
 */
enum class EnvelopePeriod(val label: String) {
    MONTHLY("Mensual"),
    BIWEEKLY("Quincenal"),
    WEEKLY("Semanal"),
    ;

    fun windowFor(date: LocalDate): PeriodWindow = when (this) {
        MONTHLY -> {
            val month = YearMonth.from(date)
            PeriodWindow(month.atDay(1), month.plusMonths(1).atDay(1))
        }

        BIWEEKLY -> {
            val month = YearMonth.from(date)
            if (date.dayOfMonth <= 15) {
                PeriodWindow(month.atDay(1), month.atDay(16))
            } else {
                PeriodWindow(month.atDay(16), month.plusMonths(1).atDay(1))
            }
        }

        WEEKLY -> {
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            PeriodWindow(monday, monday.plusWeeks(1))
        }
    }

    /** Periodo cerrado inmediatamente anterior al que contiene [date]. */
    fun previousWindow(date: LocalDate): PeriodWindow =
        windowFor(windowFor(date).start.minusDays(1))
}

/** Rango de fechas `[start, endExclusive)` de un periodo de sobre. */
data class PeriodWindow(
    val start: LocalDate,
    val endExclusive: LocalDate,
) {
    val lastDay: LocalDate get() = endExclusive.minusDays(1)

    fun startMillis(zone: ZoneId): Long = start.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endMillis(zone: ZoneId): Long = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()

    fun contains(epochMillis: Long, zone: ZoneId): Boolean {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return !date.isBefore(start) && date.isBefore(endExclusive)
    }

    /** Etiqueta corta y humana, p. ej. `1–15 oct` o `26 oct – 1 nov`. */
    fun label(): String {
        val first = start.dayOfMonth
        val last = lastDay.dayOfMonth
        return if (start.month == lastDay.month) {
            "$first–$last ${monthAbbr(start)}"
        } else {
            "$first ${monthAbbr(start)} – $last ${monthAbbr(lastDay)}"
        }
    }

    private fun monthAbbr(date: LocalDate): String =
        MONTH_FORMAT.format(date).replace(".", "")

    private companion object {
        val MONTH_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("es-AR"))
    }
}
