package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import kotlinx.coroutines.flow.Flow

/** Resumen del mes: dos flujos distintos, nunca una "suma neta" engañosa. */
data class MonthSummary(
    val spentMinor: Long,
    val incomeMinor: Long,
    /** Gastos que no se pudieron convertir por falta de cotización. */
    val missingRateExpenses: Int = 0,
)

interface TxnRepository {

    fun observeRecent(limit: Int = 20): Flow<List<TxnEntity>>

    /** Todos los movimientos (pantalla Movimientos y exportación CSV). */
    fun observeAll(): Flow<List<TxnEntity>>

    fun observeNetBalance(currency: String): Flow<Long>

    fun observeMonthSummary(
        currency: String,
        fromMillis: Long,
        toMillis: Long,
    ): Flow<MonthSummary>

    suspend fun add(txn: TxnEntity)

    /** Total de movimientos guardados, para mensajes de hito. */
    suspend fun count(): Int

    suspend fun delete(txn: TxnEntity)

    /** Borra todos los movimientos. Los sobres, pagos y perfil se conservan. */
    suspend fun deleteAll()
}
