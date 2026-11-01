package com.suarezmillan.finanzas.ui.onboarding

import androidx.lifecycle.ViewModel
import com.suarezmillan.finanzas.core.settings.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decide si el tutorial de bienvenida se muestra al perfil que acaba de entrar.
 *
 * La marca vive en [AppSettings] por `userId`, así que cada perfil ve el
 * tutorial una sola vez, aunque comparta el teléfono con otras cuentas.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: AppSettings,
) : ViewModel() {

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    /** Muestra el tutorial solo si este perfil todavía no lo vio. */
    fun sync(userId: String) {
        _visible.value = userId.isNotBlank() && !settings.hasSeenOnboarding(userId)
    }

    /** Reabrir desde Ajustes: no vuelve a marcar nada hasta que se cierre. */
    fun show() {
        _visible.value = true
    }

    /** Cerrar (Saltar, Empezar o atrás): se recuerda para este perfil. */
    fun dismiss(userId: String) {
        settings.markOnboardingSeen(userId)
        _visible.value = false
    }
}
