package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.data.local.dao.CategoryDao
import com.suarezmillan.finanzas.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryEntity>>
}

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
}
