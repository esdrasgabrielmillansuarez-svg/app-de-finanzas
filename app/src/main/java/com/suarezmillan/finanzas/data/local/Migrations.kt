package com.suarezmillan.finanzas.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: tabla de perfiles locales (`users`).
 *
 * Se escribe a mano y no con `fallbackToDestructiveMigration()` porque los
 * movimientos del usuario no son recuperables.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `users` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `email` TEXT NOT NULL,
                `passwordHash` TEXT NOT NULL,
                `salt` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `lastLoginAt` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")
    }
}
