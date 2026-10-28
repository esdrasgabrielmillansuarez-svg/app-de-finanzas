package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.data.local.dao.ScheduledPaymentDao
import com.suarezmillan.finanzas.data.local.entity.PaymentStatus
import com.suarezmillan.finanzas.data.local.entity.ScheduledPaymentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ScheduledPaymentRepository {

    fun observeUnpaid(): Flow<List<ScheduledPaymentEntity>>

    suspend fun add(payment: ScheduledPaymentEntity)

    suspend fun setStatus(id: String, status: PaymentStatus, txnId: String?)

    suspend fun delete(payment: ScheduledPaymentEntity)
}

class ScheduledPaymentRepositoryImpl @Inject constructor(
    private val dao: ScheduledPaymentDao,
) : ScheduledPaymentRepository {

    override fun observeUnpaid(): Flow<List<ScheduledPaymentEntity>> = dao.observeUnpaid()

    override suspend fun add(payment: ScheduledPaymentEntity) = dao.upsert(payment)

    override suspend fun setStatus(id: String, status: PaymentStatus, txnId: String?) =
        dao.setStatus(id, status, txnId)

    override suspend fun delete(payment: ScheduledPaymentEntity) = dao.delete(payment)
}
