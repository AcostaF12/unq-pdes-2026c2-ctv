package unq.pdes.backend.config.bootstrap

import java.math.BigDecimal

/**
 * Stable keys shared with the internal flights demo catalog. Database ids are discovered at startup.
 */
object SeedFlightIds {
    val DESTINATIONS = listOf("PAR", "LON", "ROM", "NYC", "RIO", "BCN", "CUN", "TYO", "DXB")

    private const val VARIANTS = 3

    fun key(
        destination: String,
        variant: Int,
        leg: Int,
    ): String {
        require(destination in DESTINATIONS) { "There are no seeded flights for destination $destination." }
        require(variant in 0 until VARIANTS && leg in 0..1) { "Invalid demo flight variant or leg." }
        return "ctv-demo-v1:$destination:$variant:$leg"
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

    val CITIES =
        mapOf(
            "BUE" to "Buenos Aires",
            "PAR" to "Paris",
            "LON" to "London",
            "ROM" to "Rome",
            "NYC" to "New York",
            "TYO" to "Tokyo",
            "RIO" to "Rio de Janeiro",
            "BCN" to "Barcelona",
            "CUN" to "Cancun",
            "DXB" to "Dubai",
        )

    /** Validate the whole fixture graph before persisting it. */
    fun validate() {
        val names = PACKAGES.map { it.name }.toSet()
        val buyers = BUYERS.map { it.username }.toSet()
        val usernames = (ADMINS + BUYERS + AGENCY_USERS.values).map { it.username }
        val purchases = PURCHASES.map { it.buyer to it.travelPackage }.toSet()
        require(usernames.distinct().size == usernames.size) { "Duplicate demo usernames." }
        require(AGENCY_USERS.keys == AGENCIES.toSet()) { "Every demo agency must have a seller." }
        require(PACKAGES.size == names.size) { "Duplicate demo package names." }
        require(PURCHASES.size == purchases.size) { "Duplicate demo purchases." }
        PACKAGES.forEach { seed ->
            require(seed.agency in AGENCIES && HOTELS.any { it.name == seed.hotel && it.cityCode == seed.destination })
            require(BigDecimal(seed.price).signum() > 0)
            SeedFlightIds.key(seed.destination, seed.flightVariant, 0)
        }
        require(PURCHASES.all { it.buyer in buyers && it.travelPackage in names && it.daysAgo >= 0 })
        require(REVIEWS.all { (it.buyer to it.travelPackage) in purchases && it.score in 0..10 })
        require(REVIEWS.map { it.buyer to it.travelPackage }.distinct().size == REVIEWS.size)
        require(FAVORITES.all { it.buyer in buyers && it.travelPackage in names })
        require(FAVORITES.distinct().size == FAVORITES.size)
    }

    // Public demo credential, documented in the README; not a real secret.
    const val BUYER_PASSWORD = "buyer123" // NOSONAR

    private const val AGENCY_FLECHA_TOURS = "Flecha Tours"
    private const val AGENCY_RUMBO_AUSTRAL = "Rumbo Austral"
    private const val HOTEL_MAISON_LUMIERE_BOUTIQUE = "Maison Lumière Boutique"
    private const val HOTEL_BURJ_OASIS_RESORT = "Burj Oasis Resort"
    private const val PACKAGE_PARIS_ROMANTICO = "París Romántico"
    private const val PACKAGE_LONDRES_CLASICO = "Londres Clásico"
    private const val PACKAGE_ROMA_IMPERIAL = "Roma Imperial"
    private const val PACKAGE_NUEVA_YORK_URBANO = "Nueva York Urbano"
    private const val PACKAGE_RIO_CARNAVAL = "Río Carnaval"
    private const val PACKAGE_PARIS_GOURMET_Y_VINO = "París Gourmet y Vino"
    private const val PACKAGE_TOKIO_TRADICIONAL = "Tokio Tradicional"
    private const val PACKAGE_DUBAI_LUJO_EN_EL_DESIERTO = "Dubái Lujo en el Desierto"
    private const val PACKAGE_CANCUN_TODO_INCLUIDO = "Cancún Todo Incluido"
    private const val PACKAGE_BARCELONA_GAUDI = "Barcelona Gaudí"
    private const val PACKAGE_NUEVA_YORK_EXPRESS = "Nueva York Express"
    private const val PACKAGE_ROMA_ETERNA = "Roma Eterna"
    private const val PACKAGE_LONDRES_EXPERIENCIA_REAL = "Londres Experiencia Real"
    private const val PACKAGE_RIO_PLAYA_Y_SAMBA = "Río Playa y Samba"
    private const val PACKAGE_TOKIO_NEON = "Tokio Neón"

    private const val UNSPLASH_PARAMS = "auto=format&fit=crop&w=1200&q=80"

    val AGENCIES = listOf("Despegar", "Almundo", AGENCY_FLECHA_TOURS, AGENCY_RUMBO_AUSTRAL)

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
            AGENCY_FLECHA_TOURS to UserSeed("agency3", "agency123", "Carolina", "Flecha"),
            AGENCY_RUMBO_AUSTRAL to UserSeed("agency4", "agency123", "Diego", "Austral"),
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
            "PAR" to listOf("Hotel Palacio de París", HOTEL_MAISON_LUMIERE_BOUTIQUE, "Le Petit Montmartre"),
            "LON" to listOf("Hotel Real de Londres", "The Kensington Gate", "Camden Backpackers Inn"),
            "ROM" to listOf("Hotel Villa Roma", "Palazzo Trastevere", "Hotel Termini Express"),
            "NYC" to listOf("Hotel Central Park", "Brooklyn Loft Hotel", "Times Square Tower"),
            "RIO" to listOf("Hotel Costa de Río", "Copacabana Palace Suites", "Ipanema Beach Hotel"),
            "BCN" to listOf("Hotel Ramblas Modernista", "Hotel Barceloneta Sea", "Hotel Arts Mar"),
            "CUN" to listOf("Riviera Maya Beach Resort", "Hotel Caribe Familiar", "Selva Maya Eco Lodge"),
            "TYO" to listOf("Sakura Garden Hotel", "Shibuya Sky Inn", "Ginza Imperial Hotel"),
            "DXB" to listOf(HOTEL_BURJ_OASIS_RESORT, "Desert Pearl Hotel", "Dubai Marina Skyline"),
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
            PackageSeed(PACKAGE_PARIS_ROMANTICO, "Despegar", "Hotel Palacio de París", "PAR", 0, "1500.00"),
            PackageSeed(PACKAGE_LONDRES_CLASICO, "Despegar", "Hotel Real de Londres", "LON", 0, "1800.00"),
            PackageSeed(PACKAGE_ROMA_IMPERIAL, "Despegar", "Hotel Villa Roma", "ROM", 0, "1650.00"),
            PackageSeed(PACKAGE_NUEVA_YORK_URBANO, "Almundo", "Hotel Central Park", "NYC", 0, "2200.00"),
            PackageSeed(PACKAGE_RIO_CARNAVAL, "Almundo", "Hotel Costa de Río", "RIO", 0, "1300.00"),
            PackageSeed(PACKAGE_PARIS_GOURMET_Y_VINO, "Despegar", HOTEL_MAISON_LUMIERE_BOUTIQUE, "PAR", 1, "2350.00"),
            PackageSeed(PACKAGE_TOKIO_TRADICIONAL, "Despegar", "Sakura Garden Hotel", "TYO", 1, "3400.00"),
            PackageSeed(PACKAGE_DUBAI_LUJO_EN_EL_DESIERTO, "Despegar", HOTEL_BURJ_OASIS_RESORT, "DXB", 2, "4450.00"),
            PackageSeed(PACKAGE_CANCUN_TODO_INCLUIDO, "Despegar", "Riviera Maya Beach Resort", "CUN", 0, "1950.00"),
            PackageSeed(PACKAGE_BARCELONA_GAUDI, "Despegar", "Hotel Ramblas Modernista", "BCN", 1, "1700.00"),
            PackageSeed(PACKAGE_NUEVA_YORK_EXPRESS, "Almundo", "Brooklyn Loft Hotel", "NYC", 1, "1450.00"),
            PackageSeed(PACKAGE_ROMA_ETERNA, "Almundo", "Palazzo Trastevere", "ROM", 1, "2100.00"),
            PackageSeed("Cancún Escapada Familiar", "Almundo", "Hotel Caribe Familiar", "CUN", 1, "1380.00"),
            PackageSeed(PACKAGE_LONDRES_EXPERIENCIA_REAL, "Almundo", "The Kensington Gate", "LON", 2, "2750.00"),
            PackageSeed(PACKAGE_RIO_PLAYA_Y_SAMBA, "Almundo", "Copacabana Palace Suites", "RIO", 1, "1750.00"),
            PackageSeed(PACKAGE_TOKIO_NEON, "Almundo", "Shibuya Sky Inn", "TYO", 0, "2900.00"),
            PackageSeed("Escapada a París", AGENCY_FLECHA_TOURS, "Le Petit Montmartre", "PAR", 2, "1190.00"),
            PackageSeed("Barcelona Playa y Tapas", AGENCY_FLECHA_TOURS, "Hotel Barceloneta Sea", "BCN", 0, "1250.00"),
            PackageSeed("Dubái Express", AGENCY_FLECHA_TOURS, "Desert Pearl Hotel", "DXB", 0, "3300.00"),
            PackageSeed("Cancún Aventura Maya", AGENCY_FLECHA_TOURS, "Selva Maya Eco Lodge", "CUN", 2, "1100.00"),
            PackageSeed("Roma Low Cost", AGENCY_FLECHA_TOURS, "Hotel Termini Express", "ROM", 2, "950.00"),
            PackageSeed("Nueva York Navidad", AGENCY_FLECHA_TOURS, "Times Square Tower", "NYC", 2, "3150.00"),
            PackageSeed("Londres Económico", AGENCY_FLECHA_TOURS, "Camden Backpackers Inn", "LON", 0, "900.00"),
            PackageSeed("Tokio Premium", AGENCY_RUMBO_AUSTRAL, "Ginza Imperial Hotel", "TYO", 2, "4200.00"),
            PackageSeed("Río Carioca Luxury", AGENCY_RUMBO_AUSTRAL, "Ipanema Beach Hotel", "RIO", 2, "2050.00"),
            PackageSeed("Barcelona Premium", AGENCY_RUMBO_AUSTRAL, "Hotel Arts Mar", "BCN", 2, "2600.00"),
            PackageSeed("Dubái Sunset", AGENCY_RUMBO_AUSTRAL, HOTEL_BURJ_OASIS_RESORT, "DXB", 1, "3900.00"),
            PackageSeed("París Navidad Mágica", AGENCY_RUMBO_AUSTRAL, HOTEL_MAISON_LUMIERE_BOUTIQUE, "PAR", 2, "2750.00"),
        )

    val PURCHASES =
        listOf(
            PurchaseSeed("buyer", PACKAGE_PARIS_ROMANTICO, 20),
            PurchaseSeed("buyer2", PACKAGE_NUEVA_YORK_URBANO, 10),
            PurchaseSeed("buyer", PACKAGE_ROMA_IMPERIAL, 5),
            PurchaseSeed("buyer", PACKAGE_LONDRES_CLASICO, 85),
            PurchaseSeed("buyer", PACKAGE_RIO_CARNAVAL, 110),
            PurchaseSeed("buyer", PACKAGE_TOKIO_TRADICIONAL, 165),
            PurchaseSeed("buyer", "Cancún Escapada Familiar", 60),
            PurchaseSeed("buyer2", PACKAGE_LONDRES_CLASICO, 45),
            PurchaseSeed("buyer2", PACKAGE_CANCUN_TODO_INCLUIDO, 130),
            PurchaseSeed("buyer2", "Barcelona Playa y Tapas", 80),
            PurchaseSeed("lmartinez", PACKAGE_PARIS_ROMANTICO, 33),
            PurchaseSeed("lmartinez", PACKAGE_PARIS_GOURMET_Y_VINO, 72),
            PurchaseSeed("lmartinez", PACKAGE_BARCELONA_GAUDI, 15),
            PurchaseSeed("lmartinez", PACKAGE_ROMA_ETERNA, 150),
            PurchaseSeed("tfernandez", PACKAGE_NUEVA_YORK_URBANO, 28),
            PurchaseSeed("tfernandez", PACKAGE_DUBAI_LUJO_EN_EL_DESIERTO, 100),
            PurchaseSeed("tfernandez", PACKAGE_LONDRES_EXPERIENCIA_REAL, 9),
            PurchaseSeed("tfernandez", PACKAGE_TOKIO_NEON, 175),
            PurchaseSeed("sgomez", PACKAGE_LONDRES_CLASICO, 50),
            PurchaseSeed("sgomez", PACKAGE_CANCUN_TODO_INCLUIDO, 22),
            PurchaseSeed("sgomez", PACKAGE_RIO_PLAYA_Y_SAMBA, 125),
            PurchaseSeed("sgomez", "Cancún Aventura Maya", 3),
            PurchaseSeed("jperez", PACKAGE_RIO_CARNAVAL, 65),
            PurchaseSeed("jperez", PACKAGE_NUEVA_YORK_EXPRESS, 38),
            PurchaseSeed("jperez", "Roma Low Cost", 12),
            PurchaseSeed("jperez", PACKAGE_TOKIO_TRADICIONAL, 90),
        )

    /** Every review matches a purchase above: a buyer can only review what was bought. */
    val REVIEWS =
        listOf(
            ReviewSeed("buyer", PACKAGE_PARIS_ROMANTICO, 9, "Excelente experiencia, volvería sin dudarlo."),
            ReviewSeed("buyer", PACKAGE_LONDRES_CLASICO, 7, "Muy bueno, aunque el hotel quedaba algo lejos del centro."),
            ReviewSeed("buyer2", PACKAGE_NUEVA_YORK_URBANO, 10, "Increíble, superó todas mis expectativas."),
            ReviewSeed("buyer", PACKAGE_RIO_CARNAVAL, 8, "Carnaval inolvidable y muy bien organizado."),
            ReviewSeed("buyer", PACKAGE_TOKIO_TRADICIONAL, 10, "Tokio es otro planeta. Templos, comida y trenes puntuales."),
            ReviewSeed("buyer2", PACKAGE_LONDRES_CLASICO, 6, "Lindo viaje, pero llovió todos los días y el desayuno era escaso."),
            ReviewSeed("buyer2", PACKAGE_CANCUN_TODO_INCLUIDO, 9, "Playa soñada y el todo incluido cumple de verdad."),
            ReviewSeed("lmartinez", PACKAGE_PARIS_ROMANTICO, 8, "Hotel hermoso y buena ubicación. El vuelo de vuelta salió tarde."),
            ReviewSeed("lmartinez", PACKAGE_PARIS_GOURMET_Y_VINO, 10, "La cata de vinos y las cenas fueron lo mejor del viaje."),
            ReviewSeed("lmartinez", PACKAGE_BARCELONA_GAUDI, 9, "Gaudí en persona es otra cosa. Muy recomendable."),
            ReviewSeed("tfernandez", PACKAGE_NUEVA_YORK_URBANO, 7, "Buena ubicación, aunque las habitaciones eran chicas."),
            ReviewSeed("tfernandez", PACKAGE_DUBAI_LUJO_EN_EL_DESIERTO, 10, "Lujo total, el safari por el desierto vale cada dólar."),
            ReviewSeed("tfernandez", PACKAGE_LONDRES_EXPERIENCIA_REAL, 5, "Esperaba más por el precio. La atención del hotel fue floja."),
            ReviewSeed("sgomez", PACKAGE_LONDRES_CLASICO, 8, "Itinerario completo y guías muy amables."),
            ReviewSeed("sgomez", PACKAGE_CANCUN_TODO_INCLUIDO, 4, "El hotel estaba en obras y se escuchaba todo el día."),
            ReviewSeed("sgomez", PACKAGE_RIO_PLAYA_Y_SAMBA, 9, "Copacabana de día y samba de noche. Perfecto."),
            ReviewSeed("jperez", PACKAGE_RIO_CARNAVAL, 7, "Muy animado, pero había demasiada gente en el hotel."),
            ReviewSeed("jperez", PACKAGE_NUEVA_YORK_EXPRESS, 6, "Cortito pero intenso. Hubiera querido un día más."),
            ReviewSeed("jperez", PACKAGE_TOKIO_TRADICIONAL, 9, "Organización impecable y hotel con jardín precioso."),
        )

    val FAVORITES =
        listOf(
            FavoriteSeed("buyer", PACKAGE_PARIS_ROMANTICO),
            FavoriteSeed("buyer", PACKAGE_LONDRES_CLASICO),
            FavoriteSeed("buyer", PACKAGE_ROMA_IMPERIAL),
            FavoriteSeed("buyer", PACKAGE_DUBAI_LUJO_EN_EL_DESIERTO),
            FavoriteSeed("buyer", PACKAGE_TOKIO_NEON),
            FavoriteSeed("buyer2", PACKAGE_NUEVA_YORK_URBANO),
            FavoriteSeed("buyer2", PACKAGE_RIO_CARNAVAL),
            FavoriteSeed("buyer2", PACKAGE_PARIS_GOURMET_Y_VINO),
            FavoriteSeed("lmartinez", PACKAGE_ROMA_ETERNA),
            FavoriteSeed("lmartinez", "Tokio Premium"),
            FavoriteSeed("tfernandez", "Barcelona Premium"),
            FavoriteSeed("sgomez", "Dubái Sunset"),
            FavoriteSeed("sgomez", "Nueva York Navidad"),
            FavoriteSeed("jperez", "Escapada a París"),
            FavoriteSeed("jperez", "Londres Económico"),
        )
}
