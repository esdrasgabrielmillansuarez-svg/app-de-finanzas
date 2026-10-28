package com.suarezmillan.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class AccountType { CASH, BANK, CARD, SAVINGS }

/**
 * Una cuenta pertenece a **una** divisa. No se mezclan divisas en una cuenta:
 * eso es lo que permite que "saldo" sea siempre un número exacto y no una suma
 * de tasas distintas.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val currency: String,
    val type: AccountType = AccountType.CASH,
    /** Saldo inicial (no incluye transacciones). */
    val initialMinor: Long = 0L,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
