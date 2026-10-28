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
 * Metadatos de una divisa: código, nombre, símbolo, región y países.
 *
 * El catálogo **no** guarda tasas inventadas. Las equivalencias salen de una
 * referencia real (BCV, paralelo, BCV/EUR, dolarapi, tabla global) o de una tasa
 * manual del usuario; si no hay ninguna, la UI dice "sin cotización".
 */
data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val region: Region,
    val countries: List<Country>,
)

private fun countries(vararg pairs: Pair<String, String>): List<Country> =
    pairs.map { Country(it.first, it.second) }

object CurrencyCatalog {

    /** Orden: primero las de uso probable en LatAm, después el resto por región. */
    val all: List<CurrencyInfo> = listOf(
        CurrencyInfo(
            "ARS", "Peso argentino", "\$", Region.AMERICA,
            countries("Argentina" to "🇦🇷"),
        ),
        CurrencyInfo(
            "USD", "Dólar estadounidense", "US\$", Region.AMERICA,
            countries("Estados Unidos" to "🇺🇸", "Ecuador" to "🇪🇨", "El Salvador" to "🇸🇻", "Puerto Rico" to "🇵🇷"),
        ),
        CurrencyInfo(
            "EUR", "Euro", "€", Region.EUROPA,
            countries(
                "España" to "🇪🇸", "Alemania" to "🇩🇪", "Francia" to "🇫🇷", "Italia" to "🇮🇹",
                "Portugal" to "🇵🇹", "Países Bajos" to "🇳🇱", "Bélgica" to "🇧🇪", "Austria" to "🇦🇹",
                "Irlanda" to "🇮🇪", "Grecia" to "🇬🇷", "Finlandia" to "🇫🇮",
            ),
        ),
        CurrencyInfo("BRL", "Real brasileño", "R\$", Region.AMERICA, countries("Brasil" to "🇧🇷")),
        CurrencyInfo("CLP", "Peso chileno", "\$", Region.AMERICA, countries("Chile" to "🇨🇱")),
        CurrencyInfo("COP", "Peso colombiano", "\$", Region.AMERICA, countries("Colombia" to "🇨🇴")),
        CurrencyInfo("MXN", "Peso mexicano", "\$", Region.AMERICA, countries("México" to "🇲🇽")),
        CurrencyInfo("PEN", "Sol peruano", "S/", Region.AMERICA, countries("Perú" to "🇵🇪")),
        CurrencyInfo("UYU", "Peso uruguayo", "\$U", Region.AMERICA, countries("Uruguay" to "🇺🇾")),
        CurrencyInfo("PYG", "Guaraní", "₲", Region.AMERICA, countries("Paraguay" to "🇵🇾")),
        CurrencyInfo("BOB", "Boliviano", "Bs", Region.AMERICA, countries("Bolivia" to "🇧🇴")),
        CurrencyInfo("VES", "Bolívar", "Bs.", Region.AMERICA, countries("Venezuela" to "🇻🇪")),
        // USDT no es una moneda ISO, pero en Venezuela se usa como referencia de
        // dólar digital. Su valor en VES sale de la referencia activa del bolívar.
        CurrencyInfo("USDT", "Tether (USDT)", "USDT", Region.AMERICA, countries("Venezuela" to "🇻🇪")),
        CurrencyInfo("CRC", "Colón costarricense", "₡", Region.AMERICA, countries("Costa Rica" to "🇨🇷")),
        CurrencyInfo("DOP", "Peso dominicano", "RD\$", Region.AMERICA, countries("República Dominicana" to "🇩🇴")),
        CurrencyInfo("GTQ", "Quetzal", "Q", Region.AMERICA, countries("Guatemala" to "🇬🇹")),
        CurrencyInfo("HNL", "Lempira", "L", Region.AMERICA, countries("Honduras" to "🇭🇳")),
        CurrencyInfo("NIO", "Córdoba", "C\$", Region.AMERICA, countries("Nicaragua" to "🇳🇮")),
        CurrencyInfo("PAB", "Balboa", "B/.", Region.AMERICA, countries("Panamá" to "🇵🇦")),
        CurrencyInfo("CAD", "Dólar canadiense", "C\$", Region.AMERICA, countries("Canadá" to "🇨🇦")),

        CurrencyInfo("GBP", "Libra esterlina", "£", Region.EUROPA, countries("Reino Unido" to "🇬🇧")),
        CurrencyInfo("CHF", "Franco suizo", "CHF", Region.EUROPA, countries("Suiza" to "🇨🇭")),
        CurrencyInfo("SEK", "Corona sueca", "kr", Region.EUROPA, countries("Suecia" to "🇸🇪")),
        CurrencyInfo("NOK", "Corona noruega", "kr", Region.EUROPA, countries("Noruega" to "🇳🇴")),
        CurrencyInfo("DKK", "Corona danesa", "kr", Region.EUROPA, countries("Dinamarca" to "🇩🇰")),
        CurrencyInfo("PLN", "Zloty", "zł", Region.EUROPA, countries("Polonia" to "🇵🇱")),
        CurrencyInfo("CZK", "Corona checa", "Kč", Region.EUROPA, countries("Chequia" to "🇨🇿")),
        CurrencyInfo("HUF", "Forinto", "Ft", Region.EUROPA, countries("Hungría" to "🇭🇺")),
        CurrencyInfo("RON", "Leu rumano", "lei", Region.EUROPA, countries("Rumanía" to "🇷🇴")),
        CurrencyInfo("TRY", "Lira turca", "₺", Region.EUROPA, countries("Turquía" to "🇹🇷")),
        CurrencyInfo("RUB", "Rublo", "₽", Region.EUROPA, countries("Rusia" to "🇷🇺")),
        CurrencyInfo("UAH", "Grivna", "₴", Region.EUROPA, countries("Ucrania" to "🇺🇦")),

        CurrencyInfo("JPY", "Yen", "¥", Region.ASIA, countries("Japón" to "🇯🇵")),
        CurrencyInfo("CNY", "Yuan", "¥", Region.ASIA, countries("China" to "🇨🇳")),
        CurrencyInfo("KRW", "Won", "₩", Region.ASIA, countries("Corea del Sur" to "🇰🇷")),
        CurrencyInfo("INR", "Rupia india", "₹", Region.ASIA, countries("India" to "🇮🇳")),
        CurrencyInfo("IDR", "Rupia indonesia", "Rp", Region.ASIA, countries("Indonesia" to "🇮🇩")),
        CurrencyInfo("MYR", "Ringgit", "RM", Region.ASIA, countries("Malasia" to "🇲🇾")),
        CurrencyInfo("SGD", "Dólar de Singapur", "S\$", Region.ASIA, countries("Singapur" to "🇸🇬")),
        CurrencyInfo("THB", "Baht", "฿", Region.ASIA, countries("Tailandia" to "🇹🇭")),
        CurrencyInfo("PHP", "Peso filipino", "₱", Region.ASIA, countries("Filipinas" to "🇵🇭")),
        CurrencyInfo("VND", "Dong", "₫", Region.ASIA, countries("Vietnam" to "🇻🇳")),
        CurrencyInfo("PKR", "Rupia pakistaní", "₨", Region.ASIA, countries("Pakistán" to "🇵🇰")),
        CurrencyInfo("BDT", "Taka", "৳", Region.ASIA, countries("Bangladés" to "🇧🇩")),
        CurrencyInfo("LKR", "Rupia de Sri Lanka", "Rs", Region.ASIA, countries("Sri Lanka" to "🇱🇰")),
        CurrencyInfo("ILS", "Nuevo shekel", "₪", Region.ASIA, countries("Israel" to "🇮🇱")),
        CurrencyInfo("AED", "Dirham", "AED", Region.ASIA, countries("Emiratos Árabes Unidos" to "🇦🇪")),
        CurrencyInfo("SAR", "Riyal saudí", "SAR", Region.ASIA, countries("Arabia Saudita" to "🇸🇦")),
        CurrencyInfo("QAR", "Riyal catarí", "QAR", Region.ASIA, countries("Catar" to "🇶🇦")),
        CurrencyInfo("KWD", "Dinar kuwaití", "KWD", Region.ASIA, countries("Kuwait" to "🇰🇼")),

        CurrencyInfo("ZAR", "Rand", "R", Region.AFRICA, countries("Sudáfrica" to "🇿🇦")),
        CurrencyInfo("EGP", "Libra egipcia", "E£", Region.AFRICA, countries("Egipto" to "🇪🇬")),
        CurrencyInfo("NGN", "Naira", "₦", Region.AFRICA, countries("Nigeria" to "🇳🇬")),
        CurrencyInfo("KES", "Chelín keniano", "KSh", Region.AFRICA, countries("Kenia" to "🇰🇪")),
        CurrencyInfo("MAD", "Dirham marroquí", "MAD", Region.AFRICA, countries("Marruecos" to "🇲🇦")),

        CurrencyInfo("AUD", "Dólar australiano", "A\$", Region.OCEANIA, countries("Australia" to "🇦🇺")),
        CurrencyInfo("NZD", "Dólar neozelandés", "NZ\$", Region.OCEANIA, countries("Nueva Zelanda" to "🇳🇿")),
    )

    private val byCode: Map<String, CurrencyInfo> = all.associateBy { it.code }

    fun find(code: String): CurrencyInfo? = byCode[code.trim().uppercase()]

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
