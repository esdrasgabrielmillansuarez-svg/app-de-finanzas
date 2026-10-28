package com.suarezmillan.finanzas.core.auth

/**
 * Validación del registro, separada de la UI para poder testearla.
 * Cada función devuelve el mensaje de error, o `null` si el campo está bien.
 */
object AuthValidation {

    private val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]{2,}$""")

    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    fun validateName(name: String): String? = when {
        name.isBlank() -> "Escribe tu nombre"
        name.trim().length < 2 -> "El nombre es demasiado corto"
        name.trim().length > 40 -> "Máximo 40 caracteres"
        else -> null
    }

    fun validateEmail(email: String): String? {
        val normalized = normalizeEmail(email)
        return when {
            normalized.isBlank() -> "Escribe tu correo"
            !EMAIL.matches(normalized) -> "Ese correo no parece válido"
            normalized.length > 120 -> "Máximo 120 caracteres"
            else -> null
        }
    }

    fun validatePassword(password: String): String? = when {
        password.isEmpty() -> "Escribe una contraseña"
        password.length < 6 -> "Al menos 6 caracteres"
        password.length > 128 -> "Máximo 128 caracteres"
        else -> null
    }
}
