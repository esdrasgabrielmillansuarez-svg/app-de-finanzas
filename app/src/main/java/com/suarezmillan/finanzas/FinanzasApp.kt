package com.suarezmillan.finanzas

import android.app.Application
import com.suarezmillan.finanzas.core.reminder.ReminderScheduler
import com.suarezmillan.finanzas.core.settings.AppSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinanzasApp : Application() {

    @Inject
    lateinit var settings: AppSettings

    @Inject
    lateinit var reminders: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        // Canal idempotente + programación diaria si el usuario no la apagó.
        reminders.ensureChannel()
        reminders.sync(settings.remindersEnabled.value)
    }
}
