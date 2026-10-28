package com.suarezmillan.finanzas.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.suarezmillan.finanzas.data.local.FinanzasDatabase
import com.suarezmillan.finanzas.data.local.MIGRATION_1_2
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
            .addCallback(SeedCallback)
            .addMigrations(MIGRATION_1_2)
            /*
             * SQLCipher (Fase 1, pendiente de dependencia):
             *
             *   implementation("net.zetetic:sqlcipher-android:4.6.1")
             *   implementation("androidx.sqlite:sqlite-ktx:2.4.0")
             *
             * y aquí:
             *
             *   .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray()))
             *
             * La passphrase NO va en el código: se genera una vez, se envuelve con
             * una clave del Android Keystore y se guarda cifrada en DataStore.
             */
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

/**
 * Datos de arranque: dos cuentas (una por divisa) y las categorías del MVP.
 * IDs fijos a propósito, para que los tests puedan referenciarlos.
 */
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.beginTransaction()
        try {
            db.execSQL(
                """
                INSERT INTO accounts (id, name, currency, type, initialMinor, archived, createdAt) VALUES
                ('acc-cash', 'Efectivo', 'ARS', 'CASH', 0, 0, 0),
                ('acc-usd',  'Dólares',  'USD', 'SAVINGS', 0, 0, 0)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO categories (id, name, emoji, kind, sortOrder, archived) VALUES
                ('cat-super',   'Súper',      '🛒', 'EXPENSE', 0, 0),
                ('cat-food',    'Comida',     '🍔', 'EXPENSE', 1, 0),
                ('cat-transp',  'Transporte', '🚕', 'EXPENSE', 2, 0),
                ('cat-home',    'Casa',       '🏠', 'EXPENSE', 3, 0),
                ('cat-fun',     'Ocio',       '🎬', 'EXPENSE', 4, 0),
                ('cat-health',  'Salud',      '💊', 'EXPENSE', 5, 0),
                ('cat-bills',   'Servicios',  '💡', 'EXPENSE', 6, 0),
                ('cat-salary',  'Sueldo',     '💵', 'INCOME',  0, 0),
                ('cat-free',    'Freelance',  '🧑‍💻', 'INCOME',  1, 0)
                """.trimIndent(),
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
