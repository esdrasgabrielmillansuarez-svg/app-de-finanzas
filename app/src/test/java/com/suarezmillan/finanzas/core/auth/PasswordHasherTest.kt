package com.suarezmillan.finanzas.core.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `hash y verificacion`() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("secreto-123", salt)

        assertTrue(PasswordHasher.verify("secreto-123", salt, hash))
        assertFalse(PasswordHasher.verify("secreto-124", salt, hash))
    }

    @Test
    fun `misma contrasena con distinta sal produce distinto hash`() {
        val first = PasswordHasher.newSalt()
        val second = PasswordHasher.newSalt()

        assertNotEquals(first, second)
        assertNotEquals(
            PasswordHasher.hash("abc123", first),
            PasswordHasher.hash("abc123", second),
        )
    }

    @Test
    fun `el hash no contiene la contrasena`() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("MiClaveSegura", salt)

        assertFalse(hash.contains("MiClaveSegura"))
        assertTrue(hash.length >= 32)
    }
}
