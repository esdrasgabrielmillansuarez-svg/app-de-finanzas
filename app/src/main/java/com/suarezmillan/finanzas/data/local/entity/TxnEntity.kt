package com.suarezmillan.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.suarezmillan.finanzas.core.fx.FxSource
import java.util.UUID

enum class TxnType { EXPENSE, INCOME, TRANSFER }

/**
 * Un movimiento.
 *
 * Reglas de bimoneda (no negociables):
 *  - `amountMinor` está **en su divisa original** ([currency]); nunca preconvertido.
 *  - `fxRateToBase` es la tasa usada en el momento del registro; `null` si la
 *    transacción ya está en la divisa base.
 *  - Cambiar la tasa de hoy no altera filas existentes.
 *
 * Una transferencia entre cuentas crea **dos** filas (una por divisa) unidas por
 * [transferGroupId], para que cada cuenta cuadre por separado.
 */
@Entity(
    tableName = "txns",
    indices = [
        Index("occurredAt"),
        Index("accountId"),
        Index("categoryId"),
        Index("transferGroupId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class TxnEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: TxnType,
    val amountMinor: Long,
    val currency: String,
    val accountId: String,
    val categoryId: String? = null,
    val merchant: String? = null,
    val note: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val fxRateToBase: Double? = null,
    val fxSource: FxSource = FxSource.NONE,
    val fxTimestamp: Long? = null,
    val transferGroupId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
