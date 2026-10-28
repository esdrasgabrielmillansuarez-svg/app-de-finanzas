package com.suarezmillan.finanzas.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `nombre valido e invalido`() {
        assertNull(AuthValidation.validateName("Suárez"))
        assertNotNull(AuthValidation.validateName(""))
        assertNotNull(AuthValidation.validateName("A"))
    }

    @Test
    fun `correo se normaliza y valida`() {
        assertEquals("ana@ejemplo.com", AuthValidation.normalizeEmail("  Ana@Ejemplo.COM "))

        assertNull(AuthValidation.validateEmail("ana@ejemplo.com"))
        assertNotNull(AuthValidation.validateEmail("ana@ejemplo"))
        assertNotNull(AuthValidation.validateEmail("sin-arroba.com"))
        assertNotNull(AuthValidation.validateEmail(""))
    }

    @Test
    fun `contrasena minima`() {
        assertNull(AuthValidation.validatePassword("123456"))
        assertNotNull(AuthValidation.validatePassword("12345"))
        assertNotNull(AuthValidation.validatePassword(""))
        assertNotNull(AuthValidation.validatePassword("x".repeat(129)))
    }
}
