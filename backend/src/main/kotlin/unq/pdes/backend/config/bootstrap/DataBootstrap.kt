package unq.pdes.backend.config.bootstrap

import java.math.BigDecimal
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Favorite
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.User
import unq.pdes.backend.persistence.jpa.AgencyRepository
import unq.pdes.backend.persistence.jpa.CityRepository
import unq.pdes.backend.persistence.jpa.FavoriteRepository
import unq.pdes.backend.persistence.jpa.HotelRepository
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.persistence.jpa.TravelPackageRepository
import unq.pdes.backend.persistence.jpa.UserRepository
import unq.pdes.backend.service.CityService
import unq.pdes.backend.service.HotelService
import unq.pdes.backend.service.UserService

@Component
@Profile("dev", "prod")
@Suppress("LongParameterList")
class DataBootstrap(
    private val cityService: CityService,
    private val hotelService: HotelService,
    private val hotelRepository: HotelRepository,
    private val cityRepository: CityRepository,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val agencyRepository: AgencyRepository,
    private val travelPackageRepository: TravelPackageRepository,
    private val favoriteRepository: FavoriteRepository,
    private val reviewRepository: ReviewRepository,
    private val purchaseRepository: PurchaseRepository,
    private val environment: Environment,
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        logger.info("Loading bootstrap data...")
        if (environment.activeProfiles.contains("dev")) {
            resetDevelopmentData()
        }

        loadCities()
        val hotels = loadHotels()
        loadAgencies()
        loadUsers()
        val packages = loadPackages(hotels)
        loadFavorites(packages)
        loadReviews(packages)
        loadPurchases(packages)
        logDataSummary()
    }

    private fun logDataSummary() {
        logger.info("Cities: {}", cityRepository.count())
        logger.info("Hotels: {}", hotelRepository.count())
        logger.info("Agencies: {}", agencyRepository.count())
        logger.info("Users: {}", userRepository.count())
        logger.info("Packages: {}", travelPackageRepository.count())
        logger.info("Favorites: {}", favoriteRepository.count())
        logger.info("Reviews: {}", reviewRepository.count())
        logger.info("Purchases: {}", purchaseRepository.count())
    }

    private fun resetDevelopmentData() {
        purchaseRepository.deleteAll()
        reviewRepository.deleteAll()
        favoriteRepository.deleteAll()
        travelPackageRepository.deleteAll()
        userRepository.deleteAll()
        agencyRepository.deleteAll()
        hotelRepository.deleteAll()
        cityRepository.deleteAll()
    }

    private fun loadCities() {
        listOf(
            City("BUE", "Buenos Aires"),
            City("PAR", "Paris"),
            City("LON", "London"),
            City("ROM", "Rome"),
            City("NYC", "New York"),
            City("TYO", "Tokyo"),
            City("RIO", "Rio de Janeiro"),
            City("BCN", "Barcelona"),
            City("CUN", "Cancun"),
            City("DXB", "Dubai"),
        ).forEach { city ->
            if (!cityService.existsByCode(city.code)) {
                cityService.save(city)
            }
        }
    }

    private fun loadHotels(): Map<String, Hotel> {
        if (hotelRepository.count() > 0) {
            return hotelRepository.findAll().associateBy { it.city.code }
        }
        return listOf(
            hotel("Gran Hotel Buenos Aires", "BUE", "https://images.ctv.demo/hotels/gran-hotel-buenos-aires.jpg"),
            hotel("Hotel Palacio de París", "PAR", "https://images.ctv.demo/hotels/palacio-de-paris.jpg"),
            hotel("Hotel Real de Londres", "LON", "https://images.ctv.demo/hotels/real-de-londres.jpg"),
            hotel("Hotel Villa Roma", "ROM", "https://images.ctv.demo/hotels/villa-roma.jpg"),
            hotel("Hotel Central Park", "NYC", "https://images.ctv.demo/hotels/central-park.jpg"),
            hotel("Hotel Costa de Río", "RIO", "https://images.ctv.demo/hotels/costa-de-rio.jpg"),
        ).map { hotelService.save(it) }
            .associateBy { it.city.code }
    }

    private fun hotel(name: String, cityCode: String, photoUrl: String): Hotel {
        return Hotel.Builder()
            .name(name)
            .city(cityService.findByCode(cityCode))
            .photoUrl(photoUrl)
            .build()
    }

    private fun loadAgencies() {
        listOf("Despegar", "Almundo").forEach { name ->
            if (!agencyRepository.existsByName(name)) {
                agencyRepository.save(Agency.Builder().name(name).build())
            }
        }
    }

    private fun loadUsers() {
        listOf(
            Triple("facosta", "Federico", "Acosta"),
            Triple("vferreyra", "Valentin", "Ferreyra"),
            Triple("adisanto", "Alan", "Disanto"),
        ).forEach { (username, firstName, lastName) ->
            if (!userRepository.existsByUsername(username)) {
                userService.createAdmin(username, username, firstName, lastName)
            }
        }
        if (!userRepository.existsByUsername("buyer")) {
            userService.register("buyer", "buyer123", "Bruno", "Buyer")
        }
        if (!userRepository.existsByUsername("buyer2")) {
            userService.register("buyer2", "buyer123", "Camila", "Compradora")
        }
        if (!userRepository.existsByUsername("agency")) {
            userService.createAgencyUser("agency", "agency123", "Agus", "Agency", agency("Despegar"))
        }
    }

    private fun loadPackages(hotels: Map<String, Hotel>): List<TravelPackage> {
        if (travelPackageRepository.count() > 0) {
            return travelPackageRepository.findAll()
        }
        val despegar = agency("Despegar")
        val almundo = agency("Almundo")
        return listOf(
            travelPackage(despegar, hotels.getValue("PAR"), "París Romántico", 1L, 2L, "1500.00"),
            travelPackage(despegar, hotels.getValue("LON"), "Londres Clásico", 3L, 4L, "1800.00"),
            travelPackage(despegar, hotels.getValue("ROM"), "Roma Imperial", 5L, 6L, "1650.00"),
            travelPackage(almundo, hotels.getValue("NYC"), "Nueva York Urbano", 7L, 8L, "2200.00"),
            travelPackage(almundo, hotels.getValue("RIO"), "Río Carnaval", 9L, 10L, "1300.00"),
        ).map { travelPackageRepository.save(it) }
    }

    private fun travelPackage(
        agency: Agency,
        hotel: Hotel,
        name: String,
        outboundFlightId: Long,
        returnFlightId: Long,
        price: String,
    ): TravelPackage {
        return TravelPackage.Builder()
            .agency(agency)
            .hotel(hotel)
            .origin(cityService.findByCode("BUE"))
            .destination(hotel.city)
            .name(name)
            .outboundFlightId(outboundFlightId)
            .returnFlightId(returnFlightId)
            .price(BigDecimal(price))
            .build()
    }

    private fun loadFavorites(packages: List<TravelPackage>) {
        if (favoriteRepository.count() > 0) {
            return
        }
        val buyer = user("buyer")
        val buyer2 = user("buyer2")
        listOf(
            buyer to packages[0],
            buyer to packages[1],
            buyer to packages[2],
            buyer2 to packages[3],
            buyer2 to packages[4],
        ).forEach { (buyer, travelPackage) ->
            favoriteRepository.save(
                Favorite.Builder().buyer(buyer).travelPackage(travelPackage).build(),
            )
        }
    }

    private fun loadReviews(packages: List<TravelPackage>) {
        if (reviewRepository.count() > 0) {
            return
        }
        val buyer = user("buyer")
        val buyer2 = user("buyer2")
        listOf(
            review(buyer, packages[0], 9, "Excelente experiencia, volvería sin dudarlo."),
            review(buyer, packages[1], 7, "Muy bueno, aunque el hotel quedaba algo lejos del centro."),
            review(buyer2, packages[3], 10, "Increíble, superó todas mis expectativas."),
        ).forEach { reviewRepository.save(it) }
    }

    private fun review(buyer: User, travelPackage: TravelPackage, score: Int, comment: String): Review {
        return Review.Builder()
            .buyer(buyer)
            .travelPackage(travelPackage)
            .score(score)
            .comment(comment)
            .build()
    }

    private fun loadPurchases(packages: List<TravelPackage>) {
        if (purchaseRepository.count() > 0) {
            return
        }
        val buyer = user("buyer")
        val buyer2 = user("buyer2")
        val now = LocalDateTime.now()
        listOf(
            purchase(buyer, packages[0], now.minusDays(20)),
            purchase(buyer2, packages[3], now.minusDays(10)),
            purchase(buyer, packages[2], now.minusDays(5)),
        ).forEach { purchaseRepository.save(it) }
    }

    private fun purchase(buyer: User, travelPackage: TravelPackage, purchasedAt: LocalDateTime): Purchase {
        return Purchase.Builder()
            .buyer(buyer)
            .travelPackage(travelPackage)
            .agency(travelPackage.agency)
            .purchasePrice(travelPackage.price)
            .purchasedAt(purchasedAt)
            .build()
    }

    private fun agency(name: String): Agency {
        return requireNotNull(agencyRepository.findByName(name)) { "Missing bootstrap agency: $name." }
    }

    private fun user(username: String): User {
        return userService.findByUsername(username)
    }
}
