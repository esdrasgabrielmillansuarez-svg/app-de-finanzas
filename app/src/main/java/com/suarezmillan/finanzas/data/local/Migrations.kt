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

/**
 * v2 → v3: limpieza de los datos de ejemplo que sembraba la app al instalar.
 *
 * Solo se borran las cuentas y categorías de arranque que **no** tengan
 * movimientos, sobres ni pagos asociados. Si el usuario ya las usó, esos datos
 * son suyos y se conservan.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM categories
            WHERE id IN (
                'cat-super', 'cat-food', 'cat-transp', 'cat-home', 'cat-fun',
                'cat-health', 'cat-bills', 'cat-salary', 'cat-free'
            )
              AND NOT EXISTS (SELECT 1 FROM txns WHERE txns.categoryId = categories.id)
              AND NOT EXISTS (SELECT 1 FROM envelopes WHERE envelopes.categoryId = categories.id)
              AND NOT EXISTS (
                  SELECT 1 FROM scheduled_payments
                  WHERE scheduled_payments.categoryId = categories.id
              )
            """.trimIndent(),
        )
        db.execSQL(
            """
            DELETE FROM accounts
            WHERE id IN ('acc-cash', 'acc-usd')
              AND NOT EXISTS (SELECT 1 FROM txns WHERE txns.accountId = accounts.id)
              AND NOT EXISTS (
                  SELECT 1 FROM scheduled_payments
                  WHERE scheduled_payments.accountId = accounts.id
              )
            """.trimIndent(),
        )
    }
}

/**
 * v3 → v4: guarda la divisa base usada al registrar el movimiento.
 *
 * Sin este dato no se puede convertir un gasto histórico a la divisa de un
 * sobre sin usar la tasa de hoy. Las filas anteriores quedan en `NULL` y caen
 * al conversor actual, sin inventar tasas.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE txns ADD COLUMN `fxBaseCurrency` TEXT")
    }
}

/**
 * v4 → v5: un sobre persistente por categoría, con nombre/emoji propios,
 * periodo configurable y arrastre.
 *
 * - De la tabla anterior (un sobre por categoría y mes) se conserva el último
 *   mes con límite real de cada categoría. Las filas con límite 0 eran la
 *   forma antigua de "quitar" el sobre y no se reviven.
 * - `name` y `emoji` arrancan con los de la categoría, que es lo único que la
 *   app conocía hasta ahora. `createdAt` es el momento de la migración para no
 *   inventar sobrantes de periodos anteriores.
 * - Se añaden índices compuestos para las consultas de gasto por tipo/fecha y
 *   por categoría/fecha.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `envelopes_new` (
                `id` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `emoji` TEXT NOT NULL,
                `limitMinor` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `period` TEXT NOT NULL,
                `rollover` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `envelopes_new`
                (`id`, `categoryId`, `name`, `emoji`, `limitMinor`, `currency`,
                 `period`, `rollover`, `createdAt`)
            SELECT e.`id`, e.`categoryId`, c.`name`, c.`emoji`, e.`limitMinor`,
                   e.`currency`, 'MONTHLY', 0, $now
            FROM `envelopes` e
            INNER JOIN `categories` c ON c.`id` = e.`categoryId`
            WHERE e.`limitMinor` > 0
              AND e.`monthKey` = (
                  SELECT MAX(e2.`monthKey`) FROM `envelopes` e2
                  WHERE e2.`categoryId` = e.`categoryId`
              )
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `envelopes`")
        db.execSQL("ALTER TABLE `envelopes_new` RENAME TO `envelopes`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_envelopes_categoryId` " +
                "ON `envelopes` (`categoryId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_envelopes_createdAt` " +
                "ON `envelopes` (`createdAt`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_txns_type_occurredAt` " +
                "ON `txns` (`type`, `occurredAt`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_txns_categoryId_occurredAt` " +
                "ON `txns` (`categoryId`, `occurredAt`)",
        )
    }
}
