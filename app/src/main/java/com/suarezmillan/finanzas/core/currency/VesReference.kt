package com.suarezmillan.finanzas.core.currency

/**
 * Referencias para el bolívar (VES). El usuario elige cuál usa la app; una
 * tasa manual para esa referencia siempre gana sobre la de internet.
 */
enum class VesReference(
    val id: String,
    val label: String,
    /** Cómo se lee la tasa en la UI (USDT ≈ 1 USD, no exactamente USD). */
    val unitLabel: String,
    val description: String,
) {
    BCV(
        id = "bcv",
        label = "BCV oficial",
        unitLabel = "1 USD",
        description = "Tasa oficial del Banco Central de Venezuela.",
    ),
    PARALELO(
        id = "paralelo",
        label = "Paralelo",
        unitLabel = "1 USD",
        description = "Dólar paralelo / mercado P2P.",
    ),
    USDT(
        id = "usdt",
        label = "USDT",
        unitLabel = "1 USDT",
        description = "Tether en Venezuela, estimado con el paralelo.",
    ),
    ;

    /** Etiqueta corta para badges. */
    val shortLabel: String
        get() = when (this) {
            BCV -> "BCV"
            PARALELO -> "Par."
            USDT -> "USDT"
        }

    companion object {
        fun fromId(id: String?): VesReference =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: BCV
    }
}
