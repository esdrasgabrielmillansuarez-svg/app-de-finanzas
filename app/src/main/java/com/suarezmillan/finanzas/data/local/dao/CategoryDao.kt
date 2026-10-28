package com.suarezmillan.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.suarezmillan.finanzas.data.local.entity.CategoryEntity
import com.suarezmillan.finanzas.data.local.entity.CategoryKind
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE archived = 0 AND kind = :kind ORDER BY sortOrder, name")
    fun observeByKind(kind: CategoryKind): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)
}
