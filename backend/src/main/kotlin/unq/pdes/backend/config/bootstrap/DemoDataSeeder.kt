package unq.pdes.backend.config.bootstrap

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.external.flights.ExternalDemoReservationDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Favorite
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User
import unq.pdes.backend.persistence.jpa.AgencyRepository
import unq.pdes.backend.persistence.jpa.CityRepository
import unq.pdes.backend.persistence.jpa.DemoSeedRunRepository
import unq.pdes.backend.persistence.jpa.FavoriteRepository
import unq.pdes.backend.persistence.jpa.HotelRepository
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.persistence.jpa.TravelPackageRepository
import unq.pdes.backend.persistence.jpa.UserRepository
import unq.pdes.backend.service.UserService
import java.math.BigDecimal
import java.time.LocalDateTime

/** One versioned transaction; one bulk read per table, in-memory reference maps, no destructive reset. */
@Component
class DemoDataSeeder(
    private val runs: DemoSeedRunRepository,
    private val cities: CityRepository,
    private val hotels: HotelRepository,
    private val agencies: AgencyRepository,
    private val users: UserRepository,
    private val packages: TravelPackageRepository,
    private val purchases: PurchaseRepository,
    private val reviews: ReviewRepository,
    private val favorites: FavoriteRepository,
    private val userService: UserService,
    private val flights: FlightsClient,
) {
    @Transactional
    fun seed(): Boolean {
        val run = runs.lockDemoRun()
        if (run.completedAt != null) return false
        SeedData.validate()
        val catalog = flights.seedCatalog().associateBy { it.seedKey }
        // Resolve every reference before writing anything. Flight ids are discovered, never assumed.
        SeedData.PACKAGES.forEach { seed ->
            for (leg in 0..1) {
                val key = SeedFlightIds.key(seed.destination, seed.flightVariant, leg)
                requireNotNull(catalog[key]?.id) { "Missing demo flight $key. Enable SEED_ENABLED in both services." }
                val flight = catalog.getValue(key)
                require(
                    flight.origin == (if (leg == 0) SeedData.ORIGIN_CODE else seed.destination) &&
                        flight.destination == (if (leg == 0) seed.destination else SeedData.ORIGIN_CODE),
                ) {
                    "Invalid route for demo flight $key."
                }
            }
        }
        val cityMap = cities.findAll().associateBy { it.code }.toMutableMap()
        SeedData.CITIES.forEach { (code, name) ->
            cityMap.getOrPut(code) { cities.save(City(code, name)) }
        }
        val hotelMap = hotels.findAll().associateBy { it.city.code to it.name }.toMutableMap()
        SeedData.HOTELS.forEach { seed ->
            hotelMap.getOrPut(seed.cityCode to seed.name) {
                hotels.save(
                    Hotel
                        .Builder()
                        .name(seed.name)
                        .city(cityMap.getValue(seed.cityCode))
                        .photoUrl(SeedData.photoUrl(seed))
                        .build(),
                )
            }
        }
        val agencyMap = agencies.findAll().associateBy { it.name }.toMutableMap()
        SeedData.AGENCIES.forEach { name -> agencyMap.getOrPut(name) { agencies.save(Agency.Builder().name(name).build()) } }
        val userMap = users.findAll().associateBy { it.username }.toMutableMap()
        SeedData.ADMINS.forEach { seed ->
            userMap.getOrPut(seed.username) { userService.createAdmin(seed.username, seed.password, seed.firstName, seed.lastName) }
        }
        SeedData.BUYERS.forEach { seed ->
            userMap.getOrPut(seed.username) { userService.register(seed.username, seed.password, seed.firstName, seed.lastName) }
        }
        SeedData.AGENCY_USERS.forEach { (agency, seed) ->
            userMap.getOrPut(seed.username) {
                userService.createAgencyUser(seed.username, seed.password, seed.firstName, seed.lastName, agencyMap.getValue(agency))
            }
        }
        val packageMap = packages.findAll().associateBy { it.agency.name to it.name }.toMutableMap()
        val seedPackages =
            SeedData.PACKAGES.associate { seed ->
                seed.name to
                    packageMap.getOrPut(seed.agency to seed.name) {
                        packages.save(
                            TravelPackage
                                .Builder()
                                .agency(agencyMap.getValue(seed.agency))
                                .hotel(hotelMap.getValue(seed.destination to seed.hotel))
                                .origin(cityMap.getValue(SeedData.ORIGIN_CODE))
                                .destination(cityMap.getValue(seed.destination))
                                .name(seed.name)
                                .price(BigDecimal(seed.price))
                                .outboundFlightId(catalog.getValue(SeedFlightIds.key(seed.destination, seed.flightVariant, 0)).id!!)
                                .returnFlightId(catalog.getValue(SeedFlightIds.key(seed.destination, seed.flightVariant, 1)).id!!)
                                .build(),
                        )
                    }
            }
        val purchased = purchases.findAll().map { it.buyer.id to it.travelPackage.id }.toMutableSet()
        val reservations = mutableListOf<ExternalDemoReservationDto>()
        val now = LocalDateTime.now()
        val eligibleBuyers = userMap.filterValues { it.role == Role.BUYER && it.enabled }.keys
        SeedData.PURCHASES.filter { it.buyer in eligibleBuyers }.forEachIndexed { index, seed ->
            val buyer = userMap.getValue(seed.buyer)
            val trip = seedPackages.getValue(seed.travelPackage)
            if (purchased.add(buyer.id to trip.id)) {
                purchases.save(purchase(buyer, trip, now.minusDays(seed.daysAgo).minusHours((index % 24).toLong())))
            }
            listOf(trip.outboundFlightId, trip.returnFlightId).forEachIndexed { leg, flightId ->
                reservations +=
                    ExternalDemoReservationDto(
                        key = "ctv-demo-v1:${seed.buyer}:${seed.travelPackage}:$leg",
                        flightId = flightId,
                        passengerName = "${buyer.firstName} ${buyer.lastName}",
                    )
            }
        }
        // The remote batch is atomic and idempotent. A local rollback can retry without selling twice.
        flights.reserveDemoSeats(reservations)
        val reviewed = reviews.findAll().map { it.buyer.id to it.travelPackage.id }.toMutableSet()
        reviews.saveAll(
            SeedData.REVIEWS.filter { it.buyer in eligibleBuyers }.mapNotNull { seed ->
                val buyer = userMap.getValue(seed.buyer)
                val trip = seedPackages.getValue(seed.travelPackage)
                require(buyer.id to trip.id in purchased) { "A demo review requires a purchase." }
                if (reviewed.add(buyer.id to trip.id)) {
                    Review
                        .Builder()
                        .buyer(buyer)
                        .travelPackage(trip)
                        .score(seed.score)
                        .comment(seed.comment)
                        .build()
                } else {
                    null
                }
            },
        )
        val favorited = favorites.findAll().map { it.buyer.id to it.travelPackage.id }.toMutableSet()
        favorites.saveAll(
            SeedData.FAVORITES.filter { it.buyer in eligibleBuyers }.mapNotNull { seed ->
                val buyer = userMap.getValue(seed.buyer)
                val trip = seedPackages.getValue(seed.travelPackage)
                if (favorited.add(buyer.id to trip.id)) {
                    Favorite
                        .Builder()
                        .buyer(buyer)
                        .travelPackage(trip)
                        .build()
                } else {
                    null
                }
            },
        )
        run.completedAt = now
        return true
    }

    private fun purchase(
        buyer: User,
        trip: TravelPackage,
        at: LocalDateTime,
    ): Purchase =
        Purchase
            .Builder()
            .buyer(buyer)
            .travelPackage(trip)
            .agency(trip.agency)
            .purchasePrice(trip.price)
            .purchasedAt(at)
            .build()
}
