package com.suarezmillan.finanzas.core.coach

import kotlin.math.abs

enum class CoachTone { CELEBRATE, NUDGE, WARN, CALM }

enum class CoachAction { OPEN_PLAN, OPEN_QUICK }

/** Pago que se acerca (días negativos = vencido). */
data class DuePayment(
    val name: String,
    val daysUntilDue: Int,
    val currency: String,
)

/**
 * Foto del momento financiero. Todo lo que el motor necesita saber para elegir
 * el mensaje. Sin Android, sin base de datos: se puede testear en la JVM.
 */
data class CoachContext(
    val movementCount: Int = 0,
    val spentMinor: Long = 0L,
    val budgetLimitMinor: Long = 0L,
    val daysLeftInMonth: Int = 15,
    val overBudgetEnvelopes: List<String> = emptyList(),
    val nearLimitEnvelopes: List<String> = emptyList(),
    val duePayments: List<DuePayment> = emptyList(),
    val currency: String = "ARS",
)

/**
 * Un "sticker": emoji grande + frase corta que cambia según el contexto.
 *
 * Principios (robados de las mejores notificaciones de Duolingo, pero sin
 * manipular): una idea por mensaje, tono de alguien que te quiere, cero culpa,
 * y siempre una acción concreta si hay algo que hacer.
 */
data class CoachMessage(
    val emoji: String,
    val title: String,
    val body: String,
    val tone: CoachTone,
    val action: CoachAction? = null,
    /** Clave estable de la variante, útil para animar solo cuando cambia. */
    val key: String = title,
)

object CoachEngine {

    /** Elige el mensaje de mayor prioridad para el contexto. */
    fun message(context: CoachContext, variant: Int = 0): CoachMessage {
        val v = abs(variant)

        context.duePayments
            .sortedBy { it.daysUntilDue }
            .firstOrNull()
            ?.let { payment ->
                if (payment.daysUntilDue < 0) {
                    val days = -payment.daysUntilDue
                    val daysText = if (days == 1) "un día" else "$days días"
                    return pick(
                        v,
                        listOf(
                            CoachMessage(
                                emoji = "🚨",
                                title = "${payment.name} venció",
                                body = "Quedó atrás hace $daysText. Pagalo y seguimos, sin darle más vueltas.",
                                tone = CoachTone.WARN,
                                action = CoachAction.OPEN_PLAN,
                                key = "overdue-${payment.name}",
                            ),
                            CoachMessage(
                                emoji = "🫠",
                                title = "Se pasó la fecha de ${payment.name}",
                                body = "Hace $daysText que espera. Un toque en Plan y lo resolvés.",
                                tone = CoachTone.WARN,
                                action = CoachAction.OPEN_PLAN,
                                key = "overdue-b-${payment.name}",
                            ),
                        ),
                    )
                }

                if (payment.daysUntilDue == 0) {
                    return pick(
                        v,
                        listOf(
                            CoachMessage(
                                emoji = "⏰",
                                title = "Hoy vence ${payment.name}",
                                body = "Un toque y te sacás el peso de encima. Después, a otra cosa.",
                                tone = CoachTone.WARN,
                                action = CoachAction.OPEN_PLAN,
                                key = "due-today-${payment.name}",
                            ),
                            CoachMessage(
                                emoji = "🚨",
                                title = "${payment.name} está en la cuerda floja",
                                body = "Vence hoy. Pagalo antes de que se te escape el día.",
                                tone = CoachTone.WARN,
                                action = CoachAction.OPEN_PLAN,
                                key = "due-today-b-${payment.name}",
                            ),
                        ),
                    )
                }

                if (payment.daysUntilDue <= 3) {
                    val whenText = when (payment.daysUntilDue) {
                        1 -> "mañana"
                        else -> "en ${payment.daysUntilDue} días"
                    }
                    return pick(
                        v,
                        listOf(
                            CoachMessage(
                                emoji = "📅",
                                title = "${payment.name}, $whenText",
                                body = "Agendalo ahora y liberás espacio mental. Tu yo del viernes te lo agradece.",
                                tone = CoachTone.NUDGE,
                                action = CoachAction.OPEN_PLAN,
                                key = "due-soon-${payment.name}",
                            ),
                            CoachMessage(
                                emoji = "🔔",
                                title = "${payment.name} viene $whenText",
                                body = "Voy a insistir con cariño hasta que lo pagues. Es por tu bien.",
                                tone = CoachTone.NUDGE,
                                action = CoachAction.OPEN_PLAN,
                                key = "due-soon-b-${payment.name}",
                            ),
                        ),
                    )
                }
            }

        context.overBudgetEnvelopes.firstOrNull()?.let { envelope ->
            return pick(
                v,
                listOf(
                    CoachMessage(
                        emoji = "🥵",
                        title = "Te pasaste en $envelope",
                        body = "No es el fin del mundo: movés un poco de otro sobre y listo.",
                        tone = CoachTone.WARN,
                        action = CoachAction.OPEN_PLAN,
                        key = "over-$envelope",
                    ),
                    CoachMessage(
                        emoji = "😅",
                        title = "$envelope se fue de tema",
                        body = "Pasó el límite. Ajustá el sobre o frená ahí, sin culpa.",
                        tone = CoachTone.WARN,
                        action = CoachAction.OPEN_PLAN,
                        key = "over-b-$envelope",
                    ),
                ),
            )
        }

        context.nearLimitEnvelopes.firstOrNull()?.let { envelope ->
            return pick(
                v,
                listOf(
                    CoachMessage(
                        emoji = "👀",
                        title = "$envelope está al límite",
                        body = "Te queda poco margen. Buen momento para decidir con la cabeza fría.",
                        tone = CoachTone.NUDGE,
                        action = CoachAction.OPEN_PLAN,
                        key = "near-$envelope",
                    ),
                ),
            )
        }

        if (context.movementCount == 0) {
            return pick(
                v,
                listOf(
                    CoachMessage(
                        emoji = "🌱",
                        title = "Tu primera semilla",
                        body = "Anotá un gasto y empezá a ver a dónde se va la plata. Tarda 3 segundos.",
                        tone = CoachTone.CALM,
                        action = CoachAction.OPEN_QUICK,
                        key = "empty-1",
                    ),
                    CoachMessage(
                        emoji = "🐣",
                        title = "Todo empieza con un ticket",
                        body = "El botón + está ahí abajo. Primer movimiento y arrancamos.",
                        tone = CoachTone.CALM,
                        action = CoachAction.OPEN_QUICK,
                        key = "empty-2",
                    ),
                ),
            )
        }

        if (context.budgetLimitMinor > 0L && context.spentMinor > context.budgetLimitMinor) {
            return pick(
                v,
                listOf(
                    CoachMessage(
                        emoji = "🐷",
                        title = "El presupuesto del mes ya voló",
                        body = "Tranqui: todavía podés ajustar sobres. Lo importante es verlo a tiempo.",
                        tone = CoachTone.WARN,
                        action = CoachAction.OPEN_PLAN,
                        key = "budget-over-1",
                    ),
                ),
            )
        }

        val ratio = if (context.budgetLimitMinor > 0L) {
            context.spentMinor.toFloat() / context.budgetLimitMinor
        } else {
            0f
        }

        if (context.budgetLimitMinor > 0L && ratio < 0.6f && context.daysLeftInMonth <= 7) {
            return pick(
                v,
                listOf(
                    CoachMessage(
                        emoji = "🏆",
                        title = "Vas ganando el mes",
                        body = "Quedan ${context.daysLeftInMonth} días y todavía respira el presupuesto. Guardá el resto.",
                        tone = CoachTone.CELEBRATE,
                        key = "winning-1",
                    ),
                    CoachMessage(
                        emoji = "🥂",
                        title = "Cierre prolijo a la vista",
                        body = "Menos de la mitad gastado con la recta final cerca. Eso es elegancia.",
                        tone = CoachTone.CELEBRATE,
                        key = "winning-2",
                    ),
                ),
            )
        }

        if (context.budgetLimitMinor > 0L && ratio >= 0.85f) {
            return pick(
                v,
                listOf(
                    CoachMessage(
                        emoji = "⚠️",
                        title = "Ojo con el sprint final",
                        body = "Queda poco del presupuesto y ${context.daysLeftInMonth} días por delante. Priorizá.",
                        tone = CoachTone.NUDGE,
                        action = CoachAction.OPEN_PLAN,
                        key = "tight-1",
                    ),
                ),
            )
        }

        return pick(
            v,
            listOf(
                CoachMessage(
                    emoji = "☕",
                    title = "Un movimiento a la vez",
                    body = "No necesitás ser contador: alcanza con anotar lo que pasa.",
                    tone = CoachTone.CALM,
                    key = "default-1",
                ),
                CoachMessage(
                    emoji = "🧠",
                    title = "Mirar los números no es juzgarte",
                    body = "Es tener el mapa. El mapa se actualiza con cada movimiento.",
                    tone = CoachTone.CALM,
                    key = "default-2",
                ),
                CoachMessage(
                    emoji = "🍀",
                    title = "Hoy es un buen día para ordenar",
                    body = "Dos minutos ahora te ahorran un susto a fin de mes.",
                    tone = CoachTone.CALM,
                    action = CoachAction.OPEN_QUICK,
                    key = "default-3",
                ),
            ),
        )
    }

    /**
     * Mensaje corto para el snackbar después de guardar.
     * Devuelve null si no hay nada especial que celebrar.
     */
    fun milestone(totalCount: Int): String? = when (totalCount) {
        1 -> "🌱 Primer movimiento. El hábito empieza acá."
        5 -> "🔥 Cinco anotados. Ya le tomaste la mano."
        10 -> "🚀 Diez movimientos. Esto ya tiene forma."
        25 -> "🏆 25 movimientos. Nivel persona organizada."
        50 -> "🧠 50. Tu yo del futuro aplaude."
        100 -> "💯 Cien. Nivel contador de confianza."
        else -> null
    }

    private fun pick(variant: Int, options: List<CoachMessage>): CoachMessage =
        options[variant % options.size]
}
