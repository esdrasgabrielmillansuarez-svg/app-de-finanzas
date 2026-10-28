package com.suarezmillan.finanzas.core.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash de contraseñas con PBKDF2-HMAC-SHA256.
 *
 * Reglas:
 *  - Nunca se guarda la contraseña, ni cifrada: solo el hash y su sal.
 *  - Cada usuario tiene una sal distinta (16 bytes aleatorios).
 *  - Comparación en tiempo constante (`MessageDigest.isEqual`) para no filtrar
 *    información por tiempo de respuesta.
 *
 * `java.util.Base64` (API 26) en lugar de `android.util.Base64` para poder
 * testear esto en la JVM sin emulador.
 */
object PasswordHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.getDecoder().decode(salt),
            ITERATIONS,
            KEY_LENGTH_BITS,
        )
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            Base64.getEncoder().encodeToString(factory.generateSecret(spec).encoded)
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val actual = hash(password, salt)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8),
        )
    }
}
