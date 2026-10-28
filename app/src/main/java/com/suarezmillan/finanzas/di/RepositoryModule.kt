package com.suarezmillan.finanzas.di

import com.suarezmillan.finanzas.data.repo.AccountRepository
import com.suarezmillan.finanzas.data.repo.AccountRepositoryImpl
import com.suarezmillan.finanzas.data.repo.AuthRepository
import com.suarezmillan.finanzas.data.repo.AuthRepositoryImpl
import com.suarezmillan.finanzas.data.repo.BudgetRepository
import com.suarezmillan.finanzas.data.repo.BudgetRepositoryImpl
import com.suarezmillan.finanzas.data.repo.CategoryRepository
import com.suarezmillan.finanzas.data.repo.CategoryRepositoryImpl
import com.suarezmillan.finanzas.data.repo.ScheduledPaymentRepository
import com.suarezmillan.finanzas.data.repo.ScheduledPaymentRepositoryImpl
import com.suarezmillan.finanzas.data.repo.TxnRepository
import com.suarezmillan.finanzas.data.repo.TxnRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTxnRepository(impl: TxnRepositoryImpl): TxnRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindScheduledPaymentRepository(
        impl: ScheduledPaymentRepositoryImpl,
    ): ScheduledPaymentRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
