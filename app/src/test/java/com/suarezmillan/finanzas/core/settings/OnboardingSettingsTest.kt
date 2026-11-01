package com.suarezmillan.finanzas.core.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingSettingsTest {

    @Test
    fun `el tutorial se recuerda por perfil y sobrevive al reinicio`() {
        val store = InMemoryPreferenceStore()
        val first = AppSettings(store)

        assertFalse(first.hasSeenOnboarding("perfil-a"))
        assertFalse(first.hasSeenOnboarding("perfil-b"))

        first.markOnboardingSeen("perfil-a")

        assertTrue(first.hasSeenOnboarding("perfil-a"))
        // Otro perfil en el mismo teléfono todavía no lo vio.
        assertFalse(first.hasSeenOnboarding("perfil-b"))

        // Una nueva instancia lee la misma preferencia.
        assertTrue(AppSettings(store).hasSeenOnboarding("perfil-a"))
    }

    @Test
    fun `un identificador vacio no marca el tutorial`() {
        val settings = AppSettings(InMemoryPreferenceStore())

        settings.markOnboardingSeen("")

        assertFalse(settings.hasSeenOnboarding(""))
    }
}
