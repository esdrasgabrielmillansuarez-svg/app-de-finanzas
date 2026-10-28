package com.suarezmillan.finanzas.core.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.suarezmillan.finanzas.core.settings.AppSettings
import com.suarezmillan.finanzas.data.local.dao.ScheduledPaymentDao
import java.util.concurrent.TimeUnit

/** Puente entre WorkManager (sin Hilt nativo) y los singletons de la app. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun scheduledPaymentDao(): ScheduledPaymentDao
    fun appSettings(): AppSettings
}

/**
 * Revisa una vez al día si hay pagos por vencer y publica un recordatorio.
 * No necesita red ni servidor: todo sale de Room.
 */
class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        if (!entry.appSettings().remindersEnabled.value) return Result.success()
        if (!ReminderNotifier.canNotify(applicationContext)) return Result.success()

        val until = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)
        val due = entry.scheduledPaymentDao().unpaidDue(until)
        if (due.isNotEmpty()) {
            ReminderNotifier.notifyDue(applicationContext, due)
        }
        return Result.success()
    }
}
