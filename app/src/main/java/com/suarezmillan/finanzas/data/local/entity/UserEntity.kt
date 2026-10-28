package com.suarezmillan.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Perfil local de la app.
 *
 * No hay servidor: el "registro" crea un perfil **en este teléfono**. La
 * contraseña se guarda como hash PBKDF2 con sal (ver `PasswordHasher`), nunca
 * en claro. `email` va normalizado a minúsculas y es único.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)],
)
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
)
