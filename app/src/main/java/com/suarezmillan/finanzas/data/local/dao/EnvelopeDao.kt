package com.suarezmillan.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.suarezmillan.finanzas.data.local.entity.EnvelopeEntity
import com.suarezmillan.finanzas.data.local.model.EnvelopeProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvelopeDao {

    @Query("SELECT * FROM envelopes WHERE monthKey = :monthKey")
    fun observeByMonth(monthKey: String): Flow<List<EnvelopeEntity>>

    /**
     * Límite vs. gastado real por sobre. El gasto se filtra por la divisa del
     * sobre: nunca se suman monedas distintas en una misma barra.
     */
    @Query(
        """
        SELECT e.categoryId AS categoryId,
               c.name AS name,
               c.emoji AS emoji,
               e.currency AS currency,
               e.limitMinor AS limitMinor,
               COALESCE((
                   SELECT SUM(ABS(t.amountMinor)) FROM txns t
                   WHERE t.categoryId = e.categoryId
                     AND t.type = 'EXPENSE'
                     AND t.currency = e.currency
                     AND t.occurredAt >= :fromMillis
                     AND t.occurredAt < :toMillis
               ), 0) AS spentMinor
        FROM envelopes e
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE e.monthKey = :monthKey
        ORDER BY c.sortOrder, c.name
        """,
    )
    fun observeProgress(
        monthKey: String,
        fromMillis: Long,
        toMillis: Long,
    ): Flow<List<EnvelopeProgress>>

    @Upsert
    suspend fun upsert(envelope: EnvelopeEntity)

    @Query("SELECT * FROM envelopes WHERE categoryId = :categoryId AND monthKey = :monthKey LIMIT 1")
    suspend fun find(categoryId: String, monthKey: String): EnvelopeEntity?

    @Delete
    suspend fun delete(envelope: EnvelopeEntity)
}
