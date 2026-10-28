package com.suarezmillan.finanzas.data.repo

import com.suarezmillan.finanzas.data.local.dao.AccountDao
import com.suarezmillan.finanzas.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Cuentas reales del usuario.
 *
 * La app ya no crea cuentas de ejemplo al instalar: la primera cuenta de una
 * divisa se crea cuando el usuario guarda un movimiento en esa divisa.
 */
interface AccountRepository {
    fun observeAll(): Flow<List<AccountEntity>>

    /** Devuelve la cuenta activa de [currency] o crea una mínima si no existe. */
    suspend fun ensure(currency: String): AccountEntity
}

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
) : AccountRepository {

    override fun observeAll(): Flow<List<AccountEntity>> = accountDao.observeAll()

    override suspend fun ensure(currency: String): AccountEntity {
        val normalized = currency.trim().uppercase()
        accountDao.findActiveByCurrency(normalized)?.let { return it }
        val account = AccountEntity(
            name = "Cuenta $normalized",
            currency = normalized,
        )
        accountDao.upsert(account)
        return account
    }
}
