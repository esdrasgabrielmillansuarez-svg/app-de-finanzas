package com.suarezmillan.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class DebtDirection { OWED_TO_ME, I_OWE }

/**
 * Deuda con abonos parciales.
 *
 * `paidMinor` se recalcula desde la tabla de abonos en la capa de repositorio;
 * aquí se guarda desnormalizado para poder pintar la lista sin agregar.
 */
@Entity(
    tableName = "debts",
    indices = [Index("direction"), Index("dueAt")],
)
data class DebtEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val counterparty: String,
    val direction: DebtDirection,
    val principalMinor: Long,
    val paidMinor: Long = 0L,
    val currency: String,
    val dueAt: Long? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val pendingMinor: Long get() = (principalMinor - paidMinor).coerceAtLeast(0L)
    val isSettled: Boolean get() = paidMinor >= principalMinor
}
