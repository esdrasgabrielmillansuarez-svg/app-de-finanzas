package com.suarezmillan.finanzas

import android.app.Application
import com.suarezmillan.finanzas.core.reminder.ReminderScheduler
import com.suarezmillan.finanzas.core.settings.AppSettings
import dagger.hilt.android.HiltAndroidApp
import java.io.File
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

        // Las fotos de cámara de la bandeja viven en `cacheDir/receipts` y se
        // borran al guardar o descartar. Si el proceso muere con facturas
        // pendientes, esos archivos quedan huérfanos: se limpian al arrancar.
        File(cacheDir, "receipts").listFiles()?.forEach { it.delete() }
    }
}
