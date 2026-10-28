package com.suarezmillan.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.suarezmillan.finanzas.data.local.entity.PaymentStatus
import com.suarezmillan.finanzas.data.local.entity.ScheduledPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledPaymentDao {

    /** Todo lo que vence en el rango, pagado o no (para el calendario). */
    @Query("SELECT * FROM scheduled_payments WHERE dueAt >= :fromMillis AND dueAt < :toMillis ORDER BY dueAt")
    fun observeRange(fromMillis: Long, toMillis: Long): Flow<List<ScheduledPaymentEntity>>

    /** Lo pendiente / vencido hasta [untilMillis] (para la lista de "próximos 7 días"). */
    @Query(
        """
        SELECT * FROM scheduled_payments
        WHERE status != 'PAID' AND dueAt < :untilMillis
        ORDER BY dueAt
        """,
    )
    fun observeUnpaidUntil(untilMillis: Long): Flow<List<ScheduledPaymentEntity>>

    /** Lo pendiente / vencido (para la lista de pagos del mes). */
    @Query(
        """
        SELECT * FROM scheduled_payments
        WHERE status != 'PAID'
        ORDER BY dueAt
        """,
    )
    fun observeUnpaid(): Flow<List<ScheduledPaymentEntity>>

    @Query("SELECT * FROM scheduled_payments WHERE id = :id")
    suspend fun getById(id: String): ScheduledPaymentEntity?

    @Upsert
    suspend fun upsert(payment: ScheduledPaymentEntity)

    @Query("UPDATE scheduled_payments SET status = :status, paidTxnId = :txnId WHERE id = :id")
    suspend fun setStatus(id: String, status: PaymentStatus, txnId: String?)

    @Delete
    suspend fun delete(payment: ScheduledPaymentEntity)
}
