package com.suarezmillan.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class Recurrence { NONE, WEEKLY, MONTHLY, YEARLY }

enum class PaymentStatus { PENDING, PAID, OVERDUE }

/** Pago programado (servicio, suscripción, tarjeta, cuota). */
@Entity(
    tableName = "scheduled_payments",
    indices = [Index("dueAt"), Index("status"), Index("accountId")],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ScheduledPaymentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val accountId: String,
    val categoryId: String? = null,
    val dueAt: Long,
    val recurrence: Recurrence = Recurrence.MONTHLY,
    val status: PaymentStatus = PaymentStatus.PENDING,
    /** Transacción generada al marcarlo como pagado. */
    val paidTxnId: String? = null,
    val reminderDaysBefore: Int = 1,
)
