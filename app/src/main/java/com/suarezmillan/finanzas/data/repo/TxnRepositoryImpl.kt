package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.data.local.dao.TxnDao
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class TxnRepositoryImpl @Inject constructor(
    private val txnDao: TxnDao,
) : TxnRepository {

    override fun observeRecent(limit: Int): Flow<List<TxnEntity>> =
        txnDao.observeRecent(limit).distinctUntilChanged()

    override fun observeAll(): Flow<List<TxnEntity>> =
        txnDao.observeAll().distinctUntilChanged()

    override fun observeNetBalance(currency: String): Flow<Long> =
        txnDao.observeNetBalance(currency).distinctUntilChanged()

    override fun observeMonthSummary(
        currency: String,
        fromMillis: Long,
        toMillis: Long,
    ): Flow<MonthSummary> = combine(
        txnDao.observeTotalByType(currency, TxnType.EXPENSE.name, fromMillis, toMillis),
        txnDao.observeTotalByType(currency, TxnType.INCOME.name, fromMillis, toMillis),
    ) { spent, income ->
        MonthSummary(spentMinor = spent, incomeMinor = income)
    }.distinctUntilChanged()

    override suspend fun add(txn: TxnEntity) = txnDao.upsert(txn)

    override suspend fun count(): Int = txnDao.count()

    override suspend fun delete(txn: TxnEntity) = txnDao.delete(txn)

    override suspend fun deleteAll() = txnDao.deleteAll()
}
