package com.suarezmillan.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TxnDao {

    @Query("SELECT * FROM txns ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txns ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txns WHERE id = :id")
    suspend fun getById(id: String): TxnEntity?

    /**
     * Suma firmada de una cuenta. Las piernas de transferencia se guardan con
     * signo (origen negativo, destino positivo), así que un simple SUM cuadra.
     */
    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM txns WHERE accountId = :accountId")
    fun observeAccountBalance(accountId: String): Flow<Long>

    /**
     * Saldo neto de una divisa sumando todos los movimientos.
     * Ojo: no incluye saldos iniciales de las cuentas (`accounts.initialMinor`).
     * TODO(Sprint 2): sumar iniciales con un JOIN y excluir transferencias
     * entre divisas del neto (hoy quedan dentro de su divisa, que es correcto).
     */
    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM txns WHERE currency = :currency")
    fun observeNetBalance(currency: String): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(ABS(amountMinor)), 0) FROM txns
        WHERE currency = :currency
          AND type = :type
          AND occurredAt >= :fromMillis AND occurredAt < :toMillis
        """,
    )
    fun observeTotalByType(
        currency: String,
        type: String,
        fromMillis: Long,
        toMillis: Long,
    ): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(ABS(amountMinor)), 0) FROM txns
        WHERE currency = :currency
          AND type = :type
          AND occurredAt >= :fromMillis AND occurredAt < :toMillis
          AND accountId IN (SELECT id FROM accounts WHERE archived = 0)
        """,
    )
    suspend fun totalByTypeOnce(
        currency: String,
        type: String,
        fromMillis: Long,
        toMillis: Long,
    ): Long

    @Upsert
    suspend fun upsert(txn: TxnEntity)

    @Upsert
    suspend fun upsertAll(txns: List<TxnEntity>)

    @Delete
    suspend fun delete(txn: TxnEntity)

    @Query("DELETE FROM txns")
    suspend fun deleteAll()
}
