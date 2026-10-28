package com.suarezmillan.finanzas.ui.quick

import kotlin.math.roundToLong

/**
 * Evaluador del teclado: `12+450`, `1200/2`, `3*99,5`.
 *
 * Parser recursivo propio en lugar de `eval` (no existe en Kotlin) y en lugar de
 * cualquier motor de scripting: aquí entra texto del usuario y no queremos
 * ejecutar nada.
 */
object AmountParser {

    /** Devuelve el importe en **unidades menores**, o null si la expresión no es válida. */
    fun evaluateToMinor(input: String): Long? {
        val normalized = input.replace(',', '.').trim()
        if (normalized.isEmpty()) return null
        if (normalized.any { !(it.isDigit() || it == '.' || it in "+-*/") }) return null
        return try {
            val parser = Parser(normalized)
            val value = parser.parseExpression()
            if (!parser.atEnd || !value.isFinite()) null else (value * 100).roundToLong().takeIf { it > 0 }
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    private class Parser(private val src: String) {
        private var i = 0
        val atEnd: Boolean get() = i >= src.length

        fun parseExpression(): Double {
            var value = parseTerm()
            while (!atEnd && (src[i] == '+' || src[i] == '-')) {
                val op = src[i++]
                val rhs = parseTerm()
                value = if (op == '+') value + rhs else value - rhs
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (!atEnd && (src[i] == '*' || src[i] == '/')) {
                val op = src[i++]
                val rhs = parseFactor()
                value = if (op == '*') value * rhs else value / rhs
            }
            return value
        }

        private fun parseFactor(): Double {
            if (atEnd) throw IllegalArgumentException("expresión incompleta")
            var sign = 1.0
            while (!atEnd && (src[i] == '+' || src[i] == '-')) {
                if (src[i] == '-') sign = -sign
                i++
            }
            val start = i
            while (!atEnd && (src[i].isDigit() || src[i] == '.')) i++
            if (start == i) throw IllegalArgumentException("número esperado")
            val number = src.substring(start, i).toDouble()
            return sign * number
        }
    }
}
