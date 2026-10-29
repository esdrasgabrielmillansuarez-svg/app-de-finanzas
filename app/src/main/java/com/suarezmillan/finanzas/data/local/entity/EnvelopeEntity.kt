package com.suarezmillan.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.suarezmillan.finanzas.core.budget.EnvelopePeriod
import java.util.UUID

/**
 * Sobre de presupuesto: uno por categoría de gasto.
 *
 * La categoría define **qué gastos** entran al sobre; el nombre y el emoji son
 * propios para que el usuario pueda llamarlo como quiera ("Mercado", "Salidas").
 * El límite se repite en cada [period] y, si [rollover] está activo, el sobrante
 * de periodos cerrados se suma al límite vigente.
 *
 * [createdAt] marca desde cuándo existe el sobre. Solo se arrastra el sobrante
 * de periodos en los que ya existía; nunca se inventan saldos anteriores.
 */
@Entity(
    tableName = "envelopes",
    indices = [
        Index(value = ["categoryId"], unique = true),
        Index("createdAt"),
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
    val name: String,
    val emoji: String,
    val limitMinor: Long,
    val currency: String,
    val period: EnvelopePeriod = EnvelopePeriod.MONTHLY,
    val rollover: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
