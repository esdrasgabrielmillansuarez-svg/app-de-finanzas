package com.suarezmillan.finanzas.ui.components

/** "actualizado hace 3 h": suficiente para un dato que cambia una vez al día. */
fun timeAgo(millis: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - millis) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "actualizado recién"
        minutes < 60 -> "actualizado hace $minutes min"
        minutes < 24 * 60 -> "actualizado hace ${minutes / 60} h"
        else -> "actualizado hace ${minutes / (24 * 60)} días"
    }
}
