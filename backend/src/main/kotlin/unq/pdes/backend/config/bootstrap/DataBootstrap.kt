package unq.pdes.backend.config.bootstrap

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
import java.math.BigDecimal
import java.time.LocalDateTime

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
            return hotelRepository.findAll().associateBy { it.name }
        }
        return SeedData.HOTELS
            .map { hotelService.save(hotel(it)) }
            .associateBy { it.name }
    }

    private fun hotel(seed: HotelSeed): Hotel =
        Hotel
            .Builder()
            .name(seed.name)
            .city(cityService.findByCode(seed.cityCode))
            .photoUrl(SeedData.photoUrl(seed))
            .build()

    private fun loadAgencies() {
        SeedData.AGENCIES.forEach { name ->
            if (!agencyRepository.existsByName(name)) {
                agencyRepository.save(Agency.Builder().name(name).build())
            }
        }
    }

    private fun loadUsers() {
        SeedData.ADMINS.forEach { admin ->
            if (!userRepository.existsByUsername(admin.username)) {
                userService.createAdmin(admin.username, admin.password, admin.firstName, admin.lastName)
            }
        }
        SeedData.BUYERS.forEach { buyer ->
            if (!userRepository.existsByUsername(buyer.username)) {
                userService.register(buyer.username, buyer.password, buyer.firstName, buyer.lastName)
            }
        }
        SeedData.AGENCY_USERS.forEach { (agencyName, user) ->
            if (!userRepository.existsByUsername(user.username)) {
                userService.createAgencyUser(user.username, user.password, user.firstName, user.lastName, agency(agencyName))
            }
        }
    }

    private fun loadPackages(hotels: Map<String, Hotel>): Map<String, TravelPackage> {
        if (travelPackageRepository.count() > 0) {
            return travelPackageRepository.findAll().associateBy { it.name }
        }
        return SeedData.PACKAGES
            .map { travelPackageRepository.save(travelPackage(it, hotels.getValue(it.hotel))) }
            .associateBy { it.name }
    }

    private fun travelPackage(
        seed: PackageSeed,
        hotel: Hotel,
    ): TravelPackage =
        TravelPackage
            .Builder()
            .agency(agency(seed.agency))
            .hotel(hotel)
            .origin(cityService.findByCode(SeedData.ORIGIN_CODE))
            .destination(hotel.city)
            .name(seed.name)
            .outboundFlightId(SeedFlightIds.outbound(seed.destination, seed.flightVariant))
            .returnFlightId(SeedFlightIds.inbound(seed.destination, seed.flightVariant))
            .price(BigDecimal(seed.price))
            .build()

    private fun loadFavorites(packages: Map<String, TravelPackage>) {
        if (favoriteRepository.count() > 0) {
            return
        }
        SeedData.FAVORITES.forEach { seed ->
            favoriteRepository.save(
                Favorite
                    .Builder()
                    .buyer(user(seed.buyer))
                    .travelPackage(packages.getValue(seed.travelPackage))
                    .build(),
            )
        }
    }

    private fun loadReviews(packages: Map<String, TravelPackage>) {
        if (reviewRepository.count() > 0) {
            return
        }
        SeedData.REVIEWS.forEach { seed ->
            reviewRepository.save(
                Review
                    .Builder()
                    .buyer(user(seed.buyer))
                    .travelPackage(packages.getValue(seed.travelPackage))
                    .score(seed.score)
                    .comment(seed.comment)
                    .build(),
            )
        }
    }

    private fun loadPurchases(packages: Map<String, TravelPackage>) {
        if (purchaseRepository.count() > 0) {
            return
        }
        val now = LocalDateTime.now()
        SeedData.PURCHASES.forEachIndexed { index, seed ->
            // Spread the hour so purchases made the same day are not stamped identically.
            val purchasedAt = now.minusDays(seed.daysAgo).minusHours((index % HOURS_IN_DAY).toLong())
            purchaseRepository.save(purchase(user(seed.buyer), packages.getValue(seed.travelPackage), purchasedAt))
        }
    }

    private fun purchase(
        buyer: User,
        travelPackage: TravelPackage,
        purchasedAt: LocalDateTime,
    ): Purchase =
        Purchase
            .Builder()
            .buyer(buyer)
            .travelPackage(travelPackage)
            .agency(travelPackage.agency)
            .purchasePrice(travelPackage.price)
            .purchasedAt(purchasedAt)
            .build()

    private fun agency(name: String): Agency = requireNotNull(agencyRepository.findByName(name)) { "Missing bootstrap agency: $name." }

    private fun user(username: String): User = userService.findByUsername(username)

    companion object {
        private const val HOURS_IN_DAY = 24
    }
}
