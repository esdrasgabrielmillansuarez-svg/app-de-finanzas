package com.suarezmillan.finanzas.core.currency

/** Región para agrupar la lista de divisas. */
enum class Region(val label: String) {
    AMERICA("América"),
    EUROPA("Europa"),
    ASIA("Asia"),
    AFRICA("África"),
    OCEANIA("Oceanía"),
}

data class Country(
    val name: String,
    val flag: String,
)

/**
 * Una divisa con los países que la usan y su tasa de referencia **contra USD**.
 *
 * `unitsPerUsd` responde "cuántas unidades de esta divisa compran 1 dólar".
 * Se guarda así (y no como "USD por unidad") porque es el número que se lee en
 * una casa de cambio y porque evita tasas con muchos ceros.
 *
 * Los valores son una **foto fija incluida en la app**: sin INTERNET no hay
 * cotización viva. El usuario puede fijar una tasa manual (ver `CurrencyRates`).
 */
data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val region: Region,
    val unitsPerUsd: Double,
    val countries: List<Country>,
)

private fun countries(vararg pairs: Pair<String, String>): List<Country> =
    pairs.map { Country(it.first, it.second) }

object CurrencyCatalog {

    /**
     * Divisa por defecto cuando no hay dato (USD = 1). Orden: primero las de
     * uso probable en LatAm, después el resto por región.
     */
    val all: List<CurrencyInfo> = listOf(
        CurrencyInfo(
            "ARS", "Peso argentino", "\$", Region.AMERICA, 1_050.0,
            countries("Argentina" to "🇦🇷"),
        ),
        CurrencyInfo(
            "USD", "Dólar estadounidense", "US\$", Region.AMERICA, 1.0,
            countries("Estados Unidos" to "🇺🇸", "Ecuador" to "🇪🇨", "El Salvador" to "🇸🇻", "Puerto Rico" to "🇵🇷"),
        ),
        CurrencyInfo(
            "EUR", "Euro", "€", Region.EUROPA, 0.92,
            countries(
                "España" to "🇪🇸", "Alemania" to "🇩🇪", "Francia" to "🇫🇷", "Italia" to "🇮🇹",
                "Portugal" to "🇵🇹", "Países Bajos" to "🇳🇱", "Bélgica" to "🇧🇪", "Austria" to "🇦🇹",
                "Irlanda" to "🇮🇪", "Grecia" to "🇬🇷", "Finlandia" to "🇫🇮",
            ),
        ),
        CurrencyInfo("BRL", "Real brasileño", "R\$", Region.AMERICA, 5.45, countries("Brasil" to "🇧🇷")),
        CurrencyInfo("CLP", "Peso chileno", "\$", Region.AMERICA, 950.0, countries("Chile" to "🇨🇱")),
        CurrencyInfo("COP", "Peso colombiano", "\$", Region.AMERICA, 3_950.0, countries("Colombia" to "🇨🇴")),
        CurrencyInfo("MXN", "Peso mexicano", "\$", Region.AMERICA, 17.6, countries("México" to "🇲🇽")),
        CurrencyInfo("PEN", "Sol peruano", "S/", Region.AMERICA, 3.75, countries("Perú" to "🇵🇪")),
        CurrencyInfo("UYU", "Peso uruguayo", "\$U", Region.AMERICA, 40.5, countries("Uruguay" to "🇺🇾")),
        CurrencyInfo("PYG", "Guaraní", "₲", Region.AMERICA, 7_450.0, countries("Paraguay" to "🇵🇾")),
        CurrencyInfo("BOB", "Boliviano", "Bs", Region.AMERICA, 6.9, countries("Bolivia" to "🇧🇴")),
        // Referencia de arranque (28/10/2026). En producción se reemplaza por la
        // cotización oficial del BCV en cuanto la app tiene red y la guarda en caché.
        CurrencyInfo("VES", "Bolívar", "Bs.", Region.AMERICA, 888.1183, countries("Venezuela" to "🇻🇪")),
        CurrencyInfo("CRC", "Colón costarricense", "₡", Region.AMERICA, 510.0, countries("Costa Rica" to "🇨🇷")),
        CurrencyInfo("DOP", "Peso dominicano", "RD\$", Region.AMERICA, 60.0, countries("República Dominicana" to "🇩🇴")),
        CurrencyInfo("GTQ", "Quetzal", "Q", Region.AMERICA, 7.7, countries("Guatemala" to "🇬🇹")),
        CurrencyInfo("HNL", "Lempira", "L", Region.AMERICA, 24.7, countries("Honduras" to "🇭🇳")),
        CurrencyInfo("NIO", "Córdoba", "C\$", Region.AMERICA, 36.7, countries("Nicaragua" to "🇳🇮")),
        CurrencyInfo("PAB", "Balboa", "B/.", Region.AMERICA, 1.0, countries("Panamá" to "🇵🇦")),
        CurrencyInfo("CAD", "Dólar canadiense", "C\$", Region.AMERICA, 1.36, countries("Canadá" to "🇨🇦")),

        CurrencyInfo("GBP", "Libra esterlina", "£", Region.EUROPA, 0.79, countries("Reino Unido" to "🇬🇧")),
        CurrencyInfo("CHF", "Franco suizo", "CHF", Region.EUROPA, 0.88, countries("Suiza" to "🇨🇭")),
        CurrencyInfo("SEK", "Corona sueca", "kr", Region.EUROPA, 10.5, countries("Suecia" to "🇸🇪")),
        CurrencyInfo("NOK", "Corona noruega", "kr", Region.EUROPA, 10.7, countries("Noruega" to "🇳🇴")),
        CurrencyInfo("DKK", "Corona danesa", "kr", Region.EUROPA, 6.9, countries("Dinamarca" to "🇩🇰")),
        CurrencyInfo("PLN", "Zloty", "zł", Region.EUROPA, 4.0, countries("Polonia" to "🇵🇱")),
        CurrencyInfo("CZK", "Corona checa", "Kč", Region.EUROPA, 23.0, countries("Chequia" to "🇨🇿")),
        CurrencyInfo("HUF", "Forinto", "Ft", Region.EUROPA, 360.0, countries("Hungría" to "🇭🇺")),
        CurrencyInfo("RON", "Leu rumano", "lei", Region.EUROPA, 4.6, countries("Rumanía" to "🇷🇴")),
        CurrencyInfo("TRY", "Lira turca", "₺", Region.EUROPA, 34.0, countries("Turquía" to "🇹🇷")),
        CurrencyInfo("RUB", "Rublo", "₽", Region.EUROPA, 95.0, countries("Rusia" to "🇷🇺")),
        CurrencyInfo("UAH", "Grivna", "₴", Region.EUROPA, 41.0, countries("Ucrania" to "🇺🇦")),

        CurrencyInfo("JPY", "Yen", "¥", Region.ASIA, 150.0, countries("Japón" to "🇯🇵")),
        CurrencyInfo("CNY", "Yuan", "¥", Region.ASIA, 7.2, countries("China" to "🇨🇳")),
        CurrencyInfo("KRW", "Won", "₩", Region.ASIA, 1_350.0, countries("Corea del Sur" to "🇰🇷")),
        CurrencyInfo("INR", "Rupia india", "₹", Region.ASIA, 84.0, countries("India" to "🇮🇳")),
        CurrencyInfo("IDR", "Rupia indonesia", "Rp", Region.ASIA, 15_800.0, countries("Indonesia" to "🇮🇩")),
        CurrencyInfo("MYR", "Ringgit", "RM", Region.ASIA, 4.5, countries("Malasia" to "🇲🇾")),
        CurrencyInfo("SGD", "Dólar de Singapur", "S\$", Region.ASIA, 1.34, countries("Singapur" to "🇸🇬")),
        CurrencyInfo("THB", "Baht", "฿", Region.ASIA, 34.0, countries("Tailandia" to "🇹🇭")),
        CurrencyInfo("PHP", "Peso filipino", "₱", Region.ASIA, 57.0, countries("Filipinas" to "🇵🇭")),
        CurrencyInfo("VND", "Dong", "₫", Region.ASIA, 25_400.0, countries("Vietnam" to "🇻🇳")),
        CurrencyInfo("PKR", "Rupia pakistaní", "₨", Region.ASIA, 278.0, countries("Pakistán" to "🇵🇰")),
        CurrencyInfo("BDT", "Taka", "৳", Region.ASIA, 120.0, countries("Bangladés" to "🇧🇩")),
        CurrencyInfo("LKR", "Rupia de Sri Lanka", "Rs", Region.ASIA, 295.0, countries("Sri Lanka" to "🇱🇰")),
        CurrencyInfo("ILS", "Nuevo shekel", "₪", Region.ASIA, 3.7, countries("Israel" to "🇮🇱")),
        CurrencyInfo("AED", "Dirham", "AED", Region.ASIA, 3.67, countries("Emiratos Árabes Unidos" to "🇦🇪")),
        CurrencyInfo("SAR", "Riyal saudí", "SAR", Region.ASIA, 3.75, countries("Arabia Saudita" to "🇸🇦")),
        CurrencyInfo("QAR", "Riyal catarí", "QAR", Region.ASIA, 3.64, countries("Catar" to "🇶🇦")),
        CurrencyInfo("KWD", "Dinar kuwaití", "KWD", Region.ASIA, 0.31, countries("Kuwait" to "🇰🇼")),

        CurrencyInfo("ZAR", "Rand", "R", Region.AFRICA, 18.0, countries("Sudáfrica" to "🇿🇦")),
        CurrencyInfo("EGP", "Libra egipcia", "E£", Region.AFRICA, 48.0, countries("Egipto" to "🇪🇬")),
        CurrencyInfo("NGN", "Naira", "₦", Region.AFRICA, 1_600.0, countries("Nigeria" to "🇳🇬")),
        CurrencyInfo("KES", "Chelín keniano", "KSh", Region.AFRICA, 129.0, countries("Kenia" to "🇰🇪")),
        CurrencyInfo("MAD", "Dirham marroquí", "MAD", Region.AFRICA, 9.9, countries("Marruecos" to "🇲🇦")),

        CurrencyInfo("AUD", "Dólar australiano", "A\$", Region.OCEANIA, 1.5, countries("Australia" to "🇦🇺")),
        CurrencyInfo("NZD", "Dólar neozelandés", "NZ\$", Region.OCEANIA, 1.65, countries("Nueva Zelanda" to "🇳🇿")),
    )

    private val byCode: Map<String, CurrencyInfo> = all.associateBy { it.code }

    fun find(code: String): CurrencyInfo? = byCode[code.trim().uppercase()]

    /** USD por defecto si el código no está en el catálogo (nunca rompe la UI). */
    fun unitsPerUsd(code: String): Double = find(code)?.unitsPerUsd ?: 1.0

    fun byRegion(): Map<Region, List<CurrencyInfo>> =
        Region.entries.associateWith { region -> all.filter { it.region == region } }
            .filterValues { it.isNotEmpty() }

    /** Busca por país, código o nombre. Vacío → todo el catálogo. */
    fun search(query: String): List<CurrencyInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { info ->
            info.code.lowercase().contains(q) ||
                info.name.lowercase().contains(q) ||
                info.countries.any { it.name.lowercase().contains(q) }
        }
    }
}
