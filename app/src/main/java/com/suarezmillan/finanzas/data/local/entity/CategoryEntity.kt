package com.suarezmillan.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class CategoryKind { EXPENSE, INCOME }

@Entity(
    tableName = "categories",
    indices = [Index("kind"), Index("sortOrder")],
)
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Emoji o clave de icono; el MVP usa emoji por simplicidad. */
    val emoji: String = "💸",
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)
