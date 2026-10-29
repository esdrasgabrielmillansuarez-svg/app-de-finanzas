package com.suarezmillan.finanzas.di

import android.content.Context
import androidx.room.Room
import com.suarezmillan.finanzas.data.local.FinanzasDatabase
import com.suarezmillan.finanzas.data.local.MIGRATION_1_2
import com.suarezmillan.finanzas.data.local.MIGRATION_2_3
import com.suarezmillan.finanzas.data.local.MIGRATION_3_4
import com.suarezmillan.finanzas.data.local.MIGRATION_4_5
import com.suarezmillan.finanzas.data.local.dao.AccountDao
import com.suarezmillan.finanzas.data.local.dao.CategoryDao
import com.suarezmillan.finanzas.data.local.dao.DebtDao
import com.suarezmillan.finanzas.data.local.dao.EnvelopeDao
import com.suarezmillan.finanzas.data.local.dao.ScheduledPaymentDao
import com.suarezmillan.finanzas.data.local.dao.TxnDao
import com.suarezmillan.finanzas.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinanzasDatabase =
        Room.databaseBuilder(context, FinanzasDatabase::class.java, FinanzasDatabase.NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides
    fun provideAccountDao(db: FinanzasDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: FinanzasDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTxnDao(db: FinanzasDatabase): TxnDao = db.txnDao()

    @Provides
    fun provideEnvelopeDao(db: FinanzasDatabase): EnvelopeDao = db.envelopeDao()

    @Provides
    fun provideScheduledPaymentDao(db: FinanzasDatabase): ScheduledPaymentDao =
        db.scheduledPaymentDao()

    @Provides
    fun provideDebtDao(db: FinanzasDatabase): DebtDao = db.debtDao()

    @Provides
    fun provideUserDao(db: FinanzasDatabase): UserDao = db.userDao()
}
