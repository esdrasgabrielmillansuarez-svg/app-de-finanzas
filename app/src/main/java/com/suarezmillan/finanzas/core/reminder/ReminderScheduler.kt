package com.suarezmillan.finanzas.core.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa el chequeo diario de vencimientos.
 *
 * Se usa WorkManager (no AlarmManager) porque no hace falta precisión al minuto:
 * un recordatorio a las 9:00 ± lo que decida el sistema alcanza y no requiere
 * permisos de alarmas exactas.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun ensureChannel() = ReminderNotifier.ensureChannel(context)

    fun sync(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextMorning(), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Primera corrida a las 9:00 de hoy o de mañana, nunca a las 3 de la mañana. */
    private fun millisUntilNextMorning(): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(9, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
            .coerceAtLeast(TimeUnit.MINUTES.toMillis(15))
    }

    private companion object {
        const val WORK_NAME = "recordatorios-pagos"
    }
}
