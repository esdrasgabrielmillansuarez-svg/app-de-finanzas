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

    @Query("SELECT * FROM envelopes WHERE monthKey = :monthKey")
    fun observeByMonth(monthKey: String): Flow<List<EnvelopeEntity>>

    /**
     * Sobres del mes con su categoría. El gasto se calcula en el repositorio
     * porque hay que convertir movimientos de otras divisas usando su tasa
     * histórica o, si falta, la cotización actual.
     */
    @Query(
        """
        SELECT e.categoryId AS categoryId,
               c.name AS name,
               c.emoji AS emoji,
               e.currency AS currency,
               e.limitMinor AS limitMinor
        FROM envelopes e
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE e.monthKey = :monthKey
        ORDER BY c.sortOrder, c.name
        """,
    )
    fun observeDefinitions(monthKey: String): Flow<List<EnvelopeDefinition>>

    @Upsert
    suspend fun upsert(envelope: EnvelopeEntity)

    @Query("SELECT * FROM envelopes WHERE categoryId = :categoryId AND monthKey = :monthKey LIMIT 1")
    suspend fun find(categoryId: String, monthKey: String): EnvelopeEntity?

    @Delete
    suspend fun delete(envelope: EnvelopeEntity)
}
