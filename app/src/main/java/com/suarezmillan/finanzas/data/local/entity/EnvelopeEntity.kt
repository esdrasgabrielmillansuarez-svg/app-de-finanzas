package com.suarezmillan.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Sobre de presupuesto por categoría y mes.
 *
 * `monthKey` en formato ISO truncado: `"2026-10"`. Un sobre por categoría y mes;
 * el índice único lo garantiza.
 */
@Entity(
    tableName = "envelopes",
    indices = [
        Index(value = ["categoryId", "monthKey"], unique = true),
        Index("monthKey"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EnvelopeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val monthKey: String,
    val limitMinor: Long,
    val currency: String,
    /** Cubrir un sobre desde otro deja rastro de que el límite se movió. */
    val carryFromMinor: Long = 0L,
)
