package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.core.currency.CurrencyRates
import com.suarezmillan.finanzas.core.settings.AppSettings
import com.suarezmillan.finanzas.data.local.dao.TxnDao
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class TxnRepositoryImpl @Inject constructor(
    private val txnDao: TxnDao,
    private val rates: CurrencyRates,
    private val settings: AppSettings,
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
        txnDao.observeByTypeBetween(TxnType.EXPENSE.name, fromMillis, toMillis),
        txnDao.observeByTypeBetween(TxnType.INCOME.name, fromMillis, toMillis),
        settings.ratesRevision,
    ) { expenses, incomes, _ ->
        var spent = 0L
        var missing = 0
        expenses.forEach { txn ->
            val converted = txn.absoluteAmountIn(currency, rates)
            if (converted == null) missing++ else spent += converted
        }
        val income = incomes.sumOf { it.absoluteAmountIn(currency, rates) ?: 0L }
        MonthSummary(
            spentMinor = spent,
            incomeMinor = income,
            missingRateExpenses = missing,
        )
    }.distinctUntilChanged()

    override suspend fun add(txn: TxnEntity) = txnDao.upsert(txn)

    override suspend fun count(): Int = txnDao.count()

    override suspend fun delete(txn: TxnEntity) = txnDao.delete(txn)

    override suspend fun deleteAll() = txnDao.deleteAll()
}
