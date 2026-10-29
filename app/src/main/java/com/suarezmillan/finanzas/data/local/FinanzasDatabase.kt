package com.suarezmillan.finanzas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.suarezmillan.finanzas.data.local.dao.AccountDao
import com.suarezmillan.finanzas.data.local.dao.CategoryDao
import com.suarezmillan.finanzas.data.local.dao.DebtDao
import com.suarezmillan.finanzas.data.local.dao.EnvelopeDao
import com.suarezmillan.finanzas.data.local.dao.ScheduledPaymentDao
import com.suarezmillan.finanzas.data.local.dao.TxnDao
import com.suarezmillan.finanzas.data.local.dao.UserDao
import com.suarezmillan.finanzas.data.local.entity.AccountEntity
import com.suarezmillan.finanzas.data.local.entity.CategoryEntity
import com.suarezmillan.finanzas.data.local.entity.DebtEntity
import com.suarezmillan.finanzas.data.local.entity.EnvelopeEntity
import com.suarezmillan.finanzas.data.local.entity.ScheduledPaymentEntity
import com.suarezmillan.finanzas.data.local.entity.TxnEntity
import com.suarezmillan.finanzas.data.local.entity.UserEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TxnEntity::class,
        EnvelopeEntity::class,
        ScheduledPaymentEntity::class,
        DebtEntity::class,
        UserEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FinanzasDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun txnDao(): TxnDao
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun scheduledPaymentDao(): ScheduledPaymentDao
    abstract fun debtDao(): DebtDao
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "finanzas.db"
    }
}
