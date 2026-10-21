package unq.pdes.backend.config.bootstrap

/**
 * Flight ids published by flights-service's bootstrap (keep both formulas in sync).
 *
 * Destinations are [DESTINATIONS] in order, each with three date variants (v = 0..2).
 * For destination index i and variant v: outbound id = (i * 3 + v) * 2 + 1, return id = (i * 3 + v) * 2 + 2.
 */
object SeedFlightIds {
    val DESTINATIONS = listOf("PAR", "LON", "ROM", "NYC", "RIO", "BCN", "CUN", "TYO", "DXB")

    private const val VARIANTS = 3

    fun outbound(
        destination: String,
        variant: Int,
    ): Long = pairIndex(destination, variant) * 2 + 1

    fun inbound(
        destination: String,
        variant: Int,
    ): Long = pairIndex(destination, variant) * 2 + 2

    private fun pairIndex(
        destination: String,
        variant: Int,
    ): Long {
        val destinationIndex = DESTINATIONS.indexOf(destination)
        require(destinationIndex >= 0) { "There are no seeded flights for destination $destination." }
        require(variant in 0 until VARIANTS) { "The flight variant must be between 0 and ${VARIANTS - 1}." }
        return (destinationIndex * VARIANTS + variant).toLong()
    }
}

data class HotelSeed(
    val name: String,
    val cityCode: String,
)

data class PackageSeed(
    val name: String,
    val agency: String,
    val hotel: String,
    val destination: String,
    val flightVariant: Int,
    val price: String,
)

data class UserSeed(
    val username: String,
    val password: String,
    val firstName: String,
    val lastName: String,
)

data class PurchaseSeed(
    val buyer: String,
    val travelPackage: String,
    val daysAgo: Long,
)

data class ReviewSeed(
    val buyer: String,
    val travelPackage: String,
    val score: Int,
    val comment: String,
)

data class FavoriteSeed(
    val buyer: String,
    val travelPackage: String,
)

@Suppress("LargeClass")
object SeedData {
    const val ORIGIN_CODE = "BUE"
    const val BUYER_PASSWORD = "buyer123"

    private const val UNSPLASH_PARAMS = "auto=format&fit=crop&w=1200&q=80"

    val AGENCIES = listOf("Despegar", "Almundo", "Flecha Tours", "Rumbo Austral")

    val ADMINS =
        listOf(
            UserSeed("facosta", "facosta", "Federico", "Acosta"),
            UserSeed("vferreyra", "vferreyra", "Valentin", "Ferreyra"),
            UserSeed("adisanto", "adisanto", "Alan", "Disanto"),
        )

    val BUYERS =
        listOf(
            UserSeed("buyer", BUYER_PASSWORD, "Bruno", "Buyer"),
            UserSeed("buyer2", BUYER_PASSWORD, "Camila", "Compradora"),
            UserSeed("lmartinez", BUYER_PASSWORD, "Lucía", "Martínez"),
            UserSeed("tfernandez", BUYER_PASSWORD, "Tomás", "Fernández"),
            UserSeed("sgomez", BUYER_PASSWORD, "Sofía", "Gómez"),
            UserSeed("jperez", BUYER_PASSWORD, "Julián", "Pérez"),
        )

    /** Agency users by agency name. */
    val AGENCY_USERS =
        mapOf(
            "Despegar" to UserSeed("agency", "agency123", "Agus", "Agency"),
            "Almundo" to UserSeed("agency2", "agency123", "Martina", "Almundo"),
        )

    private val CITY_PHOTOS =
        mapOf(
            "BUE" to
                listOf(
                    "photo-1725499267114-15531716d101",
                    "photo-1781041004799-481f1e2a8fad",
                    "photo-1610135206707-0f03e4800631",
                ),
            "PAR" to
                listOf(
                    "photo-1502602898657-3e91760cbb34",
                    "photo-1499856871958-5b9627545d1a",
                    "photo-1549144511-f099e773c147",
                ),
            "LON" to
                listOf(
                    "photo-1513635269975-59663e0ac1ad",
                    "photo-1486299267070-83823f5448dd",
                    "photo-1529655683826-aba9b3e77383",
                ),
            "ROM" to
                listOf(
                    "photo-1552832230-c0197dd311b5",
                    "photo-1529260830199-42c24126f198",
                    "photo-1515542622106-78bda8ba0e5b",
                ),
            "NYC" to
                listOf(
                    "photo-1496442226666-8d4d0e62e6e9",
                    "photo-1485871981521-5b1fd3805eee",
                    "photo-1480714378408-67cf0d13bc1b",
                ),
            "TYO" to
                listOf(
                    "photo-1540959733332-eab4deabeeaf",
                    "photo-1542051841857-5f90071e7989",
                    "photo-1480796927426-f609979314bd",
                ),
            "RIO" to
                listOf(
                    "photo-1772603503638-74ca65f96589",
                    "photo-1763110805416-22e7a6fffa58",
                    "photo-1518639192441-8fce0a366e2e",
                ),
            "BCN" to
                listOf(
                    "photo-1583422409516-2895a77efded",
                    "photo-1539037116277-4db20889f2d4",
                    "photo-1523531294919-4bcd7c65e216",
                ),
            "CUN" to
                listOf(
                    "photo-1507525428034-b723cf961d3e",
                    "photo-1473496169904-658ba7c44d8a",
                    "photo-1500375592092-40eb2168fd21",
                ),
            "DXB" to
                listOf(
                    "photo-1512453979798-5ea266f8880c",
                    "photo-1518684079-3c830dcef090",
                    "photo-1546412414-8035e1776c9a",
                ),
        )

    /** Three hotels per city; the photo of each one is the city photo at the same position. */
    private val HOTEL_NAMES =
        mapOf(
            "BUE" to listOf("Gran Hotel Buenos Aires", "Palermo Soho Boutique", "Puerto Madero Suites"),
            "PAR" to listOf("Hotel Palacio de París", "Maison Lumière Boutique", "Le Petit Montmartre"),
            "LON" to listOf("Hotel Real de Londres", "The Kensington Gate", "Camden Backpackers Inn"),
            "ROM" to listOf("Hotel Villa Roma", "Palazzo Trastevere", "Hotel Termini Express"),
            "NYC" to listOf("Hotel Central Park", "Brooklyn Loft Hotel", "Times Square Tower"),
            "RIO" to listOf("Hotel Costa de Río", "Copacabana Palace Suites", "Ipanema Beach Hotel"),
            "BCN" to listOf("Hotel Ramblas Modernista", "Hotel Barceloneta Sea", "Hotel Arts Mar"),
            "CUN" to listOf("Riviera Maya Beach Resort", "Hotel Caribe Familiar", "Selva Maya Eco Lodge"),
            "TYO" to listOf("Sakura Garden Hotel", "Shibuya Sky Inn", "Ginza Imperial Hotel"),
            "DXB" to listOf("Burj Oasis Resort", "Desert Pearl Hotel", "Dubai Marina Skyline"),
        )

    val HOTELS: List<HotelSeed> =
        HOTEL_NAMES.flatMap { (cityCode, names) -> names.map { HotelSeed(it, cityCode) } }

    fun photoUrl(hotel: HotelSeed): String {
        val position = HOTEL_NAMES.getValue(hotel.cityCode).indexOf(hotel.name)
        return "https://images.unsplash.com/${CITY_PHOTOS.getValue(hotel.cityCode)[position]}?$UNSPLASH_PARAMS"
    }

    /** The first five keep the original names, agencies, hotels and prices used by e2e and acceptance tests. */
    val PACKAGES =
        listOf(
            PackageSeed("París Romántico", "Despegar", "Hotel Palacio de París", "PAR", 0, "1500.00"),
            PackageSeed("Londres Clásico", "Despegar", "Hotel Real de Londres", "LON", 0, "1800.00"),
            PackageSeed("Roma Imperial", "Despegar", "Hotel Villa Roma", "ROM", 0, "1650.00"),
            PackageSeed("Nueva York Urbano", "Almundo", "Hotel Central Park", "NYC", 0, "2200.00"),
            PackageSeed("Río Carnaval", "Almundo", "Hotel Costa de Río", "RIO", 0, "1300.00"),
            PackageSeed("París Gourmet y Vino", "Despegar", "Maison Lumière Boutique", "PAR", 1, "2350.00"),
            PackageSeed("Tokio Tradicional", "Despegar", "Sakura Garden Hotel", "TYO", 1, "3400.00"),
            PackageSeed("Dubái Lujo en el Desierto", "Despegar", "Burj Oasis Resort", "DXB", 2, "4450.00"),
            PackageSeed("Cancún Todo Incluido", "Despegar", "Riviera Maya Beach Resort", "CUN", 0, "1950.00"),
            PackageSeed("Barcelona Gaudí", "Despegar", "Hotel Ramblas Modernista", "BCN", 1, "1700.00"),
            PackageSeed("Nueva York Express", "Almundo", "Brooklyn Loft Hotel", "NYC", 1, "1450.00"),
            PackageSeed("Roma Eterna", "Almundo", "Palazzo Trastevere", "ROM", 1, "2100.00"),
            PackageSeed("Cancún Escapada Familiar", "Almundo", "Hotel Caribe Familiar", "CUN", 1, "1380.00"),
            PackageSeed("Londres Experiencia Real", "Almundo", "The Kensington Gate", "LON", 2, "2750.00"),
            PackageSeed("Río Playa y Samba", "Almundo", "Copacabana Palace Suites", "RIO", 1, "1750.00"),
            PackageSeed("Tokio Neón", "Almundo", "Shibuya Sky Inn", "TYO", 0, "2900.00"),
            PackageSeed("Escapada a París", "Flecha Tours", "Le Petit Montmartre", "PAR", 2, "1190.00"),
            PackageSeed("Barcelona Playa y Tapas", "Flecha Tours", "Hotel Barceloneta Sea", "BCN", 0, "1250.00"),
            PackageSeed("Dubái Express", "Flecha Tours", "Desert Pearl Hotel", "DXB", 0, "3300.00"),
            PackageSeed("Cancún Aventura Maya", "Flecha Tours", "Selva Maya Eco Lodge", "CUN", 2, "1100.00"),
            PackageSeed("Roma Low Cost", "Flecha Tours", "Hotel Termini Express", "ROM", 2, "950.00"),
            PackageSeed("Nueva York Navidad", "Flecha Tours", "Times Square Tower", "NYC", 2, "3150.00"),
            PackageSeed("Londres Económico", "Flecha Tours", "Camden Backpackers Inn", "LON", 0, "900.00"),
            PackageSeed("Tokio Premium", "Rumbo Austral", "Ginza Imperial Hotel", "TYO", 2, "4200.00"),
            PackageSeed("Río Carioca Luxury", "Rumbo Austral", "Ipanema Beach Hotel", "RIO", 2, "2050.00"),
            PackageSeed("Barcelona Premium", "Rumbo Austral", "Hotel Arts Mar", "BCN", 2, "2600.00"),
            PackageSeed("Dubái Sunset", "Rumbo Austral", "Burj Oasis Resort", "DXB", 1, "3900.00"),
            PackageSeed("París Navidad Mágica", "Rumbo Austral", "Maison Lumière Boutique", "PAR", 2, "2750.00"),
        )

    val PURCHASES =
        listOf(
            PurchaseSeed("buyer", "París Romántico", 20),
            PurchaseSeed("buyer2", "Nueva York Urbano", 10),
            PurchaseSeed("buyer", "Roma Imperial", 5),
            PurchaseSeed("buyer", "Londres Clásico", 85),
            PurchaseSeed("buyer", "Río Carnaval", 110),
            PurchaseSeed("buyer", "Tokio Tradicional", 165),
            PurchaseSeed("buyer", "Cancún Escapada Familiar", 60),
            PurchaseSeed("buyer2", "Londres Clásico", 45),
            PurchaseSeed("buyer2", "Cancún Todo Incluido", 130),
            PurchaseSeed("buyer2", "Barcelona Playa y Tapas", 80),
            PurchaseSeed("lmartinez", "París Romántico", 33),
            PurchaseSeed("lmartinez", "París Gourmet y Vino", 72),
            PurchaseSeed("lmartinez", "Barcelona Gaudí", 15),
            PurchaseSeed("lmartinez", "Roma Eterna", 150),
            PurchaseSeed("tfernandez", "Nueva York Urbano", 28),
            PurchaseSeed("tfernandez", "Dubái Lujo en el Desierto", 100),
            PurchaseSeed("tfernandez", "Londres Experiencia Real", 9),
            PurchaseSeed("tfernandez", "Tokio Neón", 175),
            PurchaseSeed("sgomez", "Londres Clásico", 50),
            PurchaseSeed("sgomez", "Cancún Todo Incluido", 22),
            PurchaseSeed("sgomez", "Río Playa y Samba", 125),
            PurchaseSeed("sgomez", "Cancún Aventura Maya", 3),
            PurchaseSeed("jperez", "Río Carnaval", 65),
            PurchaseSeed("jperez", "Nueva York Express", 38),
            PurchaseSeed("jperez", "Roma Low Cost", 12),
            PurchaseSeed("jperez", "Tokio Tradicional", 90),
        )

    /** Every review matches a purchase above: a buyer can only review what was bought. */
    val REVIEWS =
        listOf(
            ReviewSeed("buyer", "París Romántico", 9, "Excelente experiencia, volvería sin dudarlo."),
            ReviewSeed("buyer", "Londres Clásico", 7, "Muy bueno, aunque el hotel quedaba algo lejos del centro."),
            ReviewSeed("buyer2", "Nueva York Urbano", 10, "Increíble, superó todas mis expectativas."),
            ReviewSeed("buyer", "Río Carnaval", 8, "Carnaval inolvidable y muy bien organizado."),
            ReviewSeed("buyer", "Tokio Tradicional", 10, "Tokio es otro planeta. Templos, comida y trenes puntuales."),
            ReviewSeed("buyer2", "Londres Clásico", 6, "Lindo viaje, pero llovió todos los días y el desayuno era escaso."),
            ReviewSeed("buyer2", "Cancún Todo Incluido", 9, "Playa soñada y el todo incluido cumple de verdad."),
            ReviewSeed("lmartinez", "París Romántico", 8, "Hotel hermoso y buena ubicación. El vuelo de vuelta salió tarde."),
            ReviewSeed("lmartinez", "París Gourmet y Vino", 10, "La cata de vinos y las cenas fueron lo mejor del viaje."),
            ReviewSeed("lmartinez", "Barcelona Gaudí", 9, "Gaudí en persona es otra cosa. Muy recomendable."),
            ReviewSeed("tfernandez", "Nueva York Urbano", 7, "Buena ubicación, aunque las habitaciones eran chicas."),
            ReviewSeed("tfernandez", "Dubái Lujo en el Desierto", 10, "Lujo total, el safari por el desierto vale cada dólar."),
            ReviewSeed("tfernandez", "Londres Experiencia Real", 5, "Esperaba más por el precio. La atención del hotel fue floja."),
            ReviewSeed("sgomez", "Londres Clásico", 8, "Itinerario completo y guías muy amables."),
            ReviewSeed("sgomez", "Cancún Todo Incluido", 4, "El hotel estaba en obras y se escuchaba todo el día."),
            ReviewSeed("sgomez", "Río Playa y Samba", 9, "Copacabana de día y samba de noche. Perfecto."),
            ReviewSeed("jperez", "Río Carnaval", 7, "Muy animado, pero había demasiada gente en el hotel."),
            ReviewSeed("jperez", "Nueva York Express", 6, "Cortito pero intenso. Hubiera querido un día más."),
            ReviewSeed("jperez", "Tokio Tradicional", 9, "Organización impecable y hotel con jardín precioso."),
        )

    val FAVORITES =
        listOf(
            FavoriteSeed("buyer", "París Romántico"),
            FavoriteSeed("buyer", "Londres Clásico"),
            FavoriteSeed("buyer", "Roma Imperial"),
            FavoriteSeed("buyer", "Dubái Lujo en el Desierto"),
            FavoriteSeed("buyer", "Tokio Neón"),
            FavoriteSeed("buyer2", "Nueva York Urbano"),
            FavoriteSeed("buyer2", "Río Carnaval"),
            FavoriteSeed("buyer2", "París Gourmet y Vino"),
            FavoriteSeed("lmartinez", "Roma Eterna"),
            FavoriteSeed("lmartinez", "Tokio Premium"),
            FavoriteSeed("tfernandez", "Barcelona Premium"),
            FavoriteSeed("sgomez", "Dubái Sunset"),
            FavoriteSeed("sgomez", "Nueva York Navidad"),
            FavoriteSeed("jperez", "Escapada a París"),
            FavoriteSeed("jperez", "Londres Económico"),
        )
}
