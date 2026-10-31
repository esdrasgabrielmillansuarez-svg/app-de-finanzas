package com.suarezmillan.finanzas.core.branding

import com.suarezmillan.finanzas.BuildConfig

/**
 * Identidad visible de la app.
 *
 * Un solo lugar para el nombre y la marca de creador: así no se repite ni se
 * desincroniza entre login, splash, ajustes y documentación. La versión sale de
 * `build.gradle.kts` para que nunca quede atrasada.
 */
object Branding {
    const val APP_NAME = "Finanzas"
    const val CREATOR = "Esdras Millan"
    const val CREDIT = "Creado por $CREATOR"
    val VERSION: String = BuildConfig.VERSION_NAME
}
