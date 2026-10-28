package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.data.local.dao.EnvelopeDao
import com.suarezmillan.finanzas.data.local.entity.EnvelopeEntity
import com.suarezmillan.finanzas.data.local.model.EnvelopeProgress
import kotlinx.coroutines.flow.Flow
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
) : BudgetRepository {

    override fun observeEnvelopes(monthKey: String): Flow<List<EnvelopeProgress>> {
        val month = YearMonth.parse(monthKey)
        val zone = ZoneId.systemDefault()
        val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return envelopeDao.observeProgress(monthKey, from, to)
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
