package com.suarezmillan.finanzas.data.local

import androidx.room.TypeConverter
import com.suarezmillan.finanzas.core.fx.FxSource

/**
 * Room ya convierte enums a texto por defecto, pero esa conversión revienta si
 * llega un valor desconocido (p. ej. tras un downgrade de la app). Para
 * [FxSource] preferimos degradar a `NONE` antes que tumbar la lectura de datos
 * financieros.
 */
class Converters {

    @TypeConverter
    fun fromFxSource(value: FxSource): String = value.name

    @TypeConverter
    fun toFxSource(value: String): FxSource =
        runCatching { FxSource.valueOf(value) }.getOrDefault(FxSource.NONE)
}
