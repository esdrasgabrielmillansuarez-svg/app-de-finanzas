package com.suarezmillan.finanzas.data.repo

import android.content.Context
import com.suarezmillan.finanzas.core.auth.AuthValidation
import com.suarezmillan.finanzas.core.auth.PasswordHasher
import com.suarezmillan.finanzas.data.local.dao.UserDao
import com.suarezmillan.finanzas.data.local.entity.UserEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthSession {
    /** Leyendo el perfil guardado: evita un parpadeo de login al abrir. */
    data object Loading : AuthSession

    data object LoggedOut : AuthSession

    data class LoggedIn(val user: UserEntity) : AuthSession
}

interface AuthRepository {

    val session: StateFlow<AuthSession>

    /** true si ya existe al menos un perfil en este teléfono. */
    val hasAccounts: StateFlow<Boolean>

    suspend fun register(name: String, email: String, password: String): Result<UserEntity>

    suspend fun login(email: String, password: String): Result<UserEntity>

    fun logout()
}

/**
 * Autenticación **local**. No hay servidor ni permiso de INTERNET: el registro
 * crea un perfil en el teléfono y la sesión se recuerda con SharedPreferences.
 *
 * TODO(Sprint 3): mover la sesión a DataStore y, si se quiere huella, envolver
 * la apertura en `BiometricPrompt` (la dependencia ya está declarada).
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    @ApplicationContext context: Context,
) : AuthRepository {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow<AuthSession>(AuthSession.Loading)
    override val session: StateFlow<AuthSession> = _session.asStateFlow()

    private val _hasAccounts = MutableStateFlow(false)
    override val hasAccounts: StateFlow<Boolean> = _hasAccounts.asStateFlow()

    init {
        scope.launch {
            _hasAccounts.value = userDao.count() > 0
            val savedId = prefs.getString(KEY_USER_ID, null)
            val user = savedId?.let { userDao.findById(it) }
            if (user == null) {
                if (savedId != null) prefs.edit().remove(KEY_USER_ID).apply()
                _session.value = AuthSession.LoggedOut
            } else {
                _session.value = AuthSession.LoggedIn(user)
            }
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
    ): Result<UserEntity> {
        AuthValidation.validateName(name)?.let { return Result.failure(IllegalArgumentException(it)) }
        AuthValidation.validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        AuthValidation.validatePassword(password)?.let { return Result.failure(IllegalArgumentException(it)) }

        val normalizedEmail = AuthValidation.normalizeEmail(email)
        val existing = userDao.findByEmail(normalizedEmail)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Ya hay una cuenta con ese correo en este teléfono"))
        }

        return try {
            val salt = PasswordHasher.newSalt()
            val user = UserEntity(
                name = name.trim(),
                email = normalizedEmail,
                passwordHash = PasswordHasher.hash(password, salt),
                salt = salt,
                lastLoginAt = System.currentTimeMillis(),
            )
            userDao.insert(user)
            persistSession(user)
            _hasAccounts.value = true
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("No se pudo crear la cuenta", e))
        }
    }

    override suspend fun login(email: String, password: String): Result<UserEntity> {
        AuthValidation.validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        if (password.isEmpty()) return Result.failure(IllegalArgumentException("Escribe tu contraseña"))

        val user = userDao.findByEmail(AuthValidation.normalizeEmail(email))
            ?: return Result.failure(IllegalArgumentException("Correo o contraseña incorrectos"))

        if (!PasswordHasher.verify(password, user.salt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Correo o contraseña incorrectos"))
        }

        val now = System.currentTimeMillis()
        return try {
            userDao.touchLogin(user.id, now)
            val updated = user.copy(lastLoginAt = now)
            persistSession(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("No se pudo iniciar sesión", e))
        }
    }

    override fun logout() {
        prefs.edit().remove(KEY_USER_ID).apply()
        _session.value = AuthSession.LoggedOut
    }

    private fun persistSession(user: UserEntity) {
        prefs.edit().putString(KEY_USER_ID, user.id).apply()
        _session.value = AuthSession.LoggedIn(user)
    }

    private companion object {
        const val PREFS = "finanzas_auth"
        const val KEY_USER_ID = "user_id"
    }
}
