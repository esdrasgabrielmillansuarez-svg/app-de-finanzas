package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.core.budget.EnvelopeCarry
import com.suarezmillan.finanzas.core.currency.CurrencyRates
import com.suarezmillan.finanzas.core.settings.AppSettings
import com.suarezmillan.finanzas.data.local.dao.EnvelopeDao
import com.suarezmillan.finanzas.data.local.dao.TxnDao
import com.suarezmillan.finanzas.data.local.entity.EnvelopeEntity
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.TxnType
import com.suarezmillan.finanzas.data.local.model.EnvelopeDefinition
import com.suarezmillan.finanzas.data.local.model.EnvelopeInput
import com.suarezmillan.finanzas.data.local.model.EnvelopeProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

interface BudgetRepository {

    /** Sobres vigentes para [today], con gasto del periodo y arrastre real. */
    fun observeEnvelopes(today: LocalDate = LocalDate.now()): Flow<List<EnvelopeProgress>>

    /** Crea o actualiza el sobre de una categoría conservando `id`/`createdAt`. */
    suspend fun save(input: EnvelopeInput)

    /** Elimina el sobre de una categoría; los movimientos no se tocan. */
    suspend fun remove(categoryId: String)
}

class BudgetRepositoryImpl @Inject constructor(
    private val envelopeDao: EnvelopeDao,
    private val txnDao: TxnDao,
    private val rates: CurrencyRates,
    private val settings: AppSettings,
) : BudgetRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEnvelopes(today: LocalDate): Flow<List<EnvelopeProgress>> =
        envelopeDao.observeDefinitions().flatMapLatest { definitions ->
            if (definitions.isEmpty()) {
                flowOf(emptyList())
            } else {
                val zone = ZoneId.systemDefault()
                // Desde el inicio de la ventana actual y desde la creación del
                // sobre más antiguo: así el gasto actual incluye movimientos
                // previos al sobre y el arrastre ve periodos completos.
                val from = minOf(
                    definitions.minOf { it.createdAt },
                    definitions.minOf { it.period.windowFor(today).startMillis(zone) },
                )
                val to = definitions.maxOf { it.period.windowFor(today).endMillis(zone) }

                combine(
                    txnDao.observeByTypeBetween(TxnType.EXPENSE.name, from, to),
                    settings.ratesRevision,
                ) { expenses, _ ->
                    buildProgress(definitions, expenses, today, zone)
                }
            }
        }

    override suspend fun save(input: EnvelopeInput) {
        val limit = input.limitMinor.coerceAtLeast(0L)
        if (limit == 0L) {
            // Un sobre sin límite no tiene sentido: equivale a quitarlo.
            envelopeDao.deleteByCategory(input.categoryId)
            return
        }
        val existing = envelopeDao.findByCategory(input.categoryId)
        val envelope = EnvelopeEntity(
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            categoryId = input.categoryId,
            name = input.name.trim().take(28).ifBlank { "Sobre" },
            emoji = input.emoji.ifBlank { "💸" },
            limitMinor = limit,
            currency = input.currency,
            period = input.period,
            rollover = input.rollover,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        envelopeDao.upsert(envelope)
    }

    override suspend fun remove(categoryId: String) = envelopeDao.deleteByCategory(categoryId)

    private fun buildProgress(
        definitions: List<EnvelopeDefinition>,
        expenses: List<TxnEntity>,
        today: LocalDate,
        zone: ZoneId,
    ): List<EnvelopeProgress> {
        val byCategory = expenses
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }

        return definitions.map { definition ->
            val txns = byCategory[definition.categoryId].orEmpty()
            val window = definition.period.windowFor(today)
            val spentBetween = { from: Long, to: Long ->
                txns.asSequence()
                    .filter { it.occurredAt >= from && it.occurredAt < to }
                    .sumOf { it.absoluteAmountIn(definition.currency, rates) ?: 0L }
            }
            val carry = if (definition.rollover) {
                EnvelopeCarry.carriedOver(
                    baseLimitMinor = definition.limitMinor,
                    createdAtMillis = definition.createdAt,
                    period = definition.period,
                    today = today,
                    zone = zone,
                    spentBetween = spentBetween,
                )
            } else {
                0L
            }
            EnvelopeProgress(
                categoryId = definition.categoryId,
                categoryName = definition.categoryName,
                name = definition.name,
                emoji = definition.emoji,
                currency = definition.currency,
                period = definition.period,
                rollover = definition.rollover,
                baseLimitMinor = definition.limitMinor,
                carryMinor = carry,
                limitMinor = definition.limitMinor + carry,
                spentMinor = spentBetween(window.startMillis(zone), window.endMillis(zone)),
                windowLabel = window.label(),
                windowStartMillis = window.startMillis(zone),
                windowEndMillis = window.endMillis(zone),
            )
        }
    }
}
