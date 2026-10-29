package com.suarezmillan.finanzas.core.budget

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Arrastre del sobrante entre periodos.
 *
 * Regla: un sobre solo arrastra el sobrante de un periodo si **ya existía al
 * empezar ese periodo**. El periodo en el que se creó no arrastra nada (pudo
 * crearse a mitad de mes y el sobrante no sería comparable).
 *
 * El cálculo recorre periodos completos desde la creación hasta el anterior al
 * actual y acumula `límite + arrastre - gastado`, sin bajar de cero. No se
 * inventan saldos: si no hay movimientos, el sobrante es el límite completo.
 */
object EnvelopeCarry {

    /** Tope de periodos recorridos para acotar el trabajo en sobres muy viejos. */
    const val MAX_PERIODS = 240

    fun carriedOver(
        baseLimitMinor: Long,
        createdAtMillis: Long,
        period: EnvelopePeriod,
        today: LocalDate,
        zone: ZoneId,
        maxPeriods: Int = MAX_PERIODS,
        spentBetween: (fromMillis: Long, toMillis: Long) -> Long,
    ): Long {
        if (baseLimitMinor <= 0L) return 0L

        val createdDate = Instant.ofEpochMilli(createdAtMillis).atZone(zone).toLocalDate()
        val current = period.windowFor(today)

        var carry = 0L
        var window = period.windowFor(createdDate)
        var steps = 0

        // Solo periodos ya cerrados: su fin no puede pasar del inicio del actual.
        while (steps++ < maxPeriods && !window.endExclusive.isAfter(current.start)) {
            if (!createdDate.isAfter(window.start)) {
                val spent = spentBetween(window.startMillis(zone), window.endMillis(zone))
                carry = (carry + baseLimitMinor - spent).coerceAtLeast(0L)
            }
            window = period.windowFor(window.endExclusive)
        }
        return carry
    }
}
