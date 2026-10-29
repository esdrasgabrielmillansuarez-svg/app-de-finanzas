package com.suarezmillan.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.suarezmillan.finanzas.data.local.entity.EnvelopeEntity
import com.suarezmillan.finanzas.data.local.model.EnvelopeDefinition
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvelopeDao {

    @Query("SELECT * FROM envelopes")
    fun observeAll(): Flow<List<EnvelopeEntity>>

    /**
     * Sobres con su categoría. El gasto se calcula en el repositorio porque hay
     * que convertir movimientos de otras divisas usando su tasa histórica o,
     * si falta, la cotización actual.
     */
    @Query(
        """
        SELECT e.categoryId AS categoryId,
               c.name AS categoryName,
               c.emoji AS categoryEmoji,
               e.name AS name,
               e.emoji AS emoji,
               e.currency AS currency,
               e.limitMinor AS limitMinor,
               e.period AS period,
               e.rollover AS rollover,
               e.createdAt AS createdAt
        FROM envelopes e
        INNER JOIN categories c ON c.id = e.categoryId
        ORDER BY c.sortOrder, c.name
        """,
    )
    fun observeDefinitions(): Flow<List<EnvelopeDefinition>>

    @Upsert
    suspend fun upsert(envelope: EnvelopeEntity)

    @Query("SELECT * FROM envelopes WHERE categoryId = :categoryId LIMIT 1")
    suspend fun findByCategory(categoryId: String): EnvelopeEntity?

    @Query("DELETE FROM envelopes WHERE categoryId = :categoryId")
    suspend fun deleteByCategory(categoryId: String)

    @Delete
    suspend fun delete(envelope: EnvelopeEntity)
}
