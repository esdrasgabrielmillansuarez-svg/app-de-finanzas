package com.suarezmillan.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.suarezmillan.finanzas.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY createdAt")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Query(
        "SELECT * FROM accounts WHERE currency = :currency AND archived = 0 " +
            "ORDER BY createdAt LIMIT 1",
    )
    suspend fun findActiveByCurrency(currency: String): AccountEntity?

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)
}
