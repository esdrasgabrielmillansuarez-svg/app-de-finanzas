package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.core.currency.CurrencyRates
import com.suarezmillan.finanzas.core.settings.AppSettings
import com.suarezmillan.finanzas.data.local.dao.EnvelopeDao
import com.suarezmillan.finanzas.data.local.dao.TxnDao
import com.suarezmillan.finanzas.data.local.entity.EnvelopeEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import com.suarezmillan.finanzas.data.local.model.EnvelopeProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

interface BudgetRepository {

    fun observeEnvelopes(monthKey: String): Flow<List<EnvelopeProgress>>

    suspend fun upsert(envelope: EnvelopeEntity)

    /**
     * Define (o reemplaza) el límite de un sobre. Busca por categoría + mes en
     * lugar de confiar en el `id` para no violar el índice único
     * (categoryId, monthKey) cuando el sobre ya existe.
     */
    suspend fun setLimit(
        categoryId: String,
        monthKey: String,
        limitMinor: Long,
        currency: String,
    )
}

class BudgetRepositoryImpl @Inject constructor(
    private val envelopeDao: EnvelopeDao,
    private val txnDao: TxnDao,
    private val rates: CurrencyRates,
    private val settings: AppSettings,
) : BudgetRepository {

    override fun observeEnvelopes(monthKey: String): Flow<List<EnvelopeProgress>> {
        val month = YearMonth.parse(monthKey)
        val zone = ZoneId.systemDefault()
        val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        return combine(
            envelopeDao.observeDefinitions(monthKey),
            txnDao.observeByTypeBetween(TxnType.EXPENSE.name, from, to),
            settings.ratesRevision,
        ) { definitions, expenses, _ ->
            val byCategory = expenses
                .filter { it.categoryId != null }
                .groupBy { it.categoryId!! }

            definitions.map { envelope ->
                val spent = byCategory[envelope.categoryId]
                    .orEmpty()
                    .sumOf { txn -> txn.absoluteAmountIn(envelope.currency, rates) ?: 0L }
                EnvelopeProgress(
                    categoryId = envelope.categoryId,
                    name = envelope.name,
                    emoji = envelope.emoji,
                    currency = envelope.currency,
                    limitMinor = envelope.limitMinor,
                    spentMinor = spent,
                )
            }
        }
    }

    override suspend fun upsert(envelope: EnvelopeEntity) = envelopeDao.upsert(envelope)

    override suspend fun setLimit(
        categoryId: String,
        monthKey: String,
        limitMinor: Long,
        currency: String,
    ) {
        val existing = envelopeDao.find(categoryId, monthKey)
        val envelope = existing?.copy(limitMinor = limitMinor)
            ?: EnvelopeEntity(
                categoryId = categoryId,
                monthKey = monthKey,
                limitMinor = limitMinor,
                currency = currency,
            )
        envelopeDao.upsert(envelope)
    }
}

/** `2026-10` para el mes de [epochMillis] en la zona del dispositivo. */
fun monthKeyOf(epochMillis: Long): String =
    YearMonth.from(java.time.Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
        .toString()
