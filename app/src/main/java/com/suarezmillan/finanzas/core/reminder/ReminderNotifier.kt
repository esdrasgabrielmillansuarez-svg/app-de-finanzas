package com.suarezmillan.finanzas.core.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suarezmillan.finanzas.MainActivity
import com.suarezmillan.finanzas.R
import com.suarezmillan.finanzas.data.local.entity.ScheduledPaymentEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Notificaciones de pagos próximos.
 *
 * Una sola notificación resumen (nunca una por pago) y con tono de recordatorio,
 * no de cobranza. Si el usuario desactivó recordatorios o el sistema no permite
 * notificar, no se publica nada.
 */
object ReminderNotifier {

    const val CHANNEL_ID = "recordatorios"
    private const val NOTIFICATION_ID = 2001

    fun ensureChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT,
        )
            .setName("Recordatorios de pagos")
            .setDescription("Avisos amables de vencimientos y pagos programados.")
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notifyDue(context: Context, payments: List<ScheduledPaymentEntity>) {
        if (payments.isEmpty() || !canNotify(context)) return

        val sorted = payments.sortedBy { it.dueAt }
        val today = LocalDate.now()

        val title: String
        val body: String
        if (sorted.size == 1) {
            val payment = sorted.first()
            title = "${emojiFor(daysUntil(payment, today))} ${payment.name} ${label(daysUntil(payment, today))}"
            body = "Abrí la app y marcalo como pagado en un toque."
        } else {
            title = "🔔 Tenés ${sorted.size} pagos cerca"
            body = sorted.take(3).joinToString(" · ") { payment ->
                "${payment.name} ${label(daysUntil(payment, today))}"
            } + if (sorted.size > 3) " y ${sorted.size - 3} más" else ""
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Sin permiso POST_NOTIFICATIONS (API 33+): se ignora, no se cae la app.
        }
    }

    private fun daysUntil(payment: ScheduledPaymentEntity, today: LocalDate): Long {
        val dueDate = Instant.ofEpochMilli(payment.dueAt).atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(today, dueDate)
    }

    private fun label(days: Long): String = when {
        days < 0 -> "venció hace ${-days} d"
        days == 0L -> "vence hoy"
        days == 1L -> "vence mañana"
        else -> "vence en $days días"
    }

    private fun emojiFor(days: Long): String = when {
        days <= 0 -> "🚨"
        days <= 3 -> "⏰"
        else -> "📅"
    }
}
