package com.suarezmillan.finanzas.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.core.auth.AuthValidation

/**
 * Entrada a la app: **Entrar** o **Crear cuenta**.
 *
 * Todo es local. La pantalla lo dice sin letra chica: no hay servidor, no hay
 * recuperación de contraseña por correo y nada sale del teléfono.
 */
@Composable
fun AuthScreen(
    busy: Boolean,
    error: String?,
    hasAccounts: Boolean,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit,
    onClearError: () -> Unit,
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // La contraseña no se guarda en el Bundle: se pierde al rotar, nunca se persiste.
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(hasAccounts) {
        if (!hasAccounts) registerMode = true
    }

    val displayError = localError ?: error

    fun clearErrors() {
        localError = null
        onClearError()
    }

    fun submit() {
        val validationError = if (registerMode) {
            AuthValidation.validateName(name)
                ?: AuthValidation.validateEmail(email)
                ?: AuthValidation.validatePassword(password)
        } else {
            AuthValidation.validateEmail(email) ?: AuthValidation.validatePassword(password)
        }
        if (validationError != null) {
            localError = validationError
            return
        }
        clearErrors()
        if (registerMode) onRegister(name, email, password) else onLogin(email, password)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(52.dp),
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Text("💸", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Finanzas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = if (registerMode) "Crea tu perfil local" else "Bienvenido de nuevo",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(22.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !registerMode,
                onClick = {
                    registerMode = false
                    clearErrors()
                },
                label = { Text("Entrar") },
            )
            FilterChip(
                selected = registerMode,
                onClick = {
                    registerMode = true
                    clearErrors()
                },
                label = { Text("Crear cuenta") },
            )
        }

        Spacer(Modifier.height(14.dp))

        if (registerMode) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(40)
                    clearErrors()
                },
                label = { Text("Nombre") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )
            Spacer(Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it.take(120)
                clearErrors()
            },
            label = { Text("Correo") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = if (registerMode) ImeAction.Next else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it.take(128)
                clearErrors()
            },
            label = { Text("Contraseña") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        if (registerMode) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Mínimo 6 caracteres. No hay recuperación por correo: si la olvidas, pierdes el acceso al perfil.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (displayError != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = displayError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { submit() },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = if (registerMode) "Crear cuenta" else "Entrar",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Sin conexión, sin servidores y sin permiso de Internet: " +
                "tu perfil y tus movimientos se quedan en este teléfono.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}
