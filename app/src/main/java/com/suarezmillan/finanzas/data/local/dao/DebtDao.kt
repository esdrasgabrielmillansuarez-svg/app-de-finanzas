package com.suarezmillan.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.suarezmillan.finanzas.data.local.entity.DebtDirection
import com.suarezmillan.finanzas.data.local.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts WHERE direction = :direction ORDER BY (paidMinor >= principalMinor), dueAt IS NULL, dueAt")
    fun observeByDirection(direction: DebtDirection): Flow<List<DebtEntity>>

    @Upsert
    suspend fun upsert(debt: DebtEntity)

    /** Abono parcial; nunca deja `paidMinor` por encima del principal. */
    @Query("UPDATE debts SET paidMinor = MIN(principalMinor, paidMinor + :deltaMinor) WHERE id = :id")
    suspend fun addPayment(id: String, deltaMinor: Long)

    @Delete
    suspend fun delete(debt: DebtEntity)
}
