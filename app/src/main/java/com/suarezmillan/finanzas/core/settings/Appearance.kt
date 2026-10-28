package com.suarezmillan.finanzas.core.settings

/** Modo de color elegido por el usuario. */
enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "Sistema"),
    LIGHT("light", "Claro"),
    DARK("dark", "Oscuro"),
    ;

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/**
 * Temas de la app.
 *
 * Regla que no se rompe: el tema cambia **la marca y las superficies de acento**,
 * nunca la semántica de divisa (USD siempre ámbar, moneda local siempre cian) ni
 * la de flujo (ingreso verde, gasto coral). Así el color sigue significando algo.
 */
enum class ThemePreset(val id: String, val label: String) {
    GRAFITO("grafito", "Grafito"),
    OCEANO("oceano", "Océano"),
    BOSQUE("bosque", "Bosque"),
    ATARDECER("atardecer", "Atardecer"),
    LAVANDA("lavanda", "Lavanda"),
    MONO("mono", "Monocromo"),
    ;

    companion object {
        fun fromId(id: String?): ThemePreset = entries.firstOrNull { it.id == id } ?: GRAFITO
    }
}
