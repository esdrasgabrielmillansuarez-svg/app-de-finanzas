package com.suarezmillan.finanzas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suarezmillan.finanzas.data.repo.AuthSession
import com.suarezmillan.finanzas.ui.auth.AuthScreen
import com.suarezmillan.finanzas.ui.auth.AuthViewModel

/**
 * Puerta de entrada: mientras se lee el perfil guardado muestra un splash
 * mínimo (sin pedir login dos veces), y después decide entre login y la app.
 */
@Composable
fun RootGate(authVm: AuthViewModel = hiltViewModel()) {
    val session by authVm.session.collectAsStateWithLifecycle()
    val busy by authVm.busy.collectAsStateWithLifecycle()
    val error by authVm.error.collectAsStateWithLifecycle()
    val hasAccounts by authVm.hasAccounts.collectAsStateWithLifecycle()

    when (val current = session) {
        AuthSession.Loading -> Splash()

        AuthSession.LoggedOut -> AuthScreen(
            busy = busy,
            error = error,
            hasAccounts = hasAccounts,
            onLogin = authVm::login,
            onRegister = authVm::register,
            onClearError = authVm::clearError,
        )

        is AuthSession.LoggedIn -> FinanzasRoot(
            userName = current.user.name,
            userEmail = current.user.email,
            onLogout = { authVm.logout() },
        )
    }
}

@Composable
private fun Splash() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Finanzas", style = MaterialTheme.typography.headlineMedium)
        Text(
            "tus cuentas, sin nube",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}
