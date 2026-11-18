package unq.pdes.backend.tests.integration.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import unq.pdes.backend.config.bootstrap.DemoDataSeeder
import unq.pdes.backend.config.bootstrap.SeedFlightIds
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.City
import unq.pdes.backend.model.DemoSeedRun
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.persistence.jpa.CityRepository
import unq.pdes.backend.persistence.jpa.DemoSeedRunRepository
import unq.pdes.backend.persistence.jpa.FavoriteRepository
import unq.pdes.backend.persistence.jpa.HotelRepository
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.persistence.jpa.TravelPackageRepository
import unq.pdes.backend.persistence.jpa.UserRepository
import unq.pdes.backend.service.UserService
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
class DemoDataSeederTest {
    @Autowired private lateinit var userService: UserService

    @Autowired private lateinit var seeder: DemoDataSeeder

    @Autowired private lateinit var runs: DemoSeedRunRepository

    @Autowired private lateinit var users: UserRepository

    @Autowired private lateinit var packages: TravelPackageRepository

    @Autowired private lateinit var purchases: PurchaseRepository

    @Autowired private lateinit var reviews: ReviewRepository

    @Autowired private lateinit var favorites: FavoriteRepository

    @Autowired private lateinit var cities: CityRepository

    @Autowired private lateinit var hotels: HotelRepository

    @Autowired private lateinit var cleanup: DataServiceH2

    @MockitoBean private lateinit var flights: FlightsClient

    @BeforeEach
    fun setup() {
        cleanup.deleteAll()
        runs.saveAndFlush(DemoSeedRun())
        Mockito.`when`(flights.seedCatalog()).thenReturn(catalog())
    }

    @AfterEach
    fun teardown() = cleanup.deleteAll()

    private fun catalog(): List<ExternalFlightDto> =
        SeedFlightIds.DESTINATIONS.flatMapIndexed { index, destination ->
            (0..2).flatMap { variant ->
                (0..1).map { leg ->
                    ExternalFlightDto(
                        1001L + index * 6 + variant * 2 + leg,
                        "Demo",
                        LocalDate.now().plusDays(30L + leg * 7),
                        LocalTime.of(8, 0),
                        if (leg == 0) "BUE" else destination,
                        if (leg == 0) destination else "BUE",
                        180,
                        180,
                        SeedFlightIds.key(destination, variant, leg),
                    )
                }
            }
        }

    @Test
    fun `seed creates the complete graph with discovered flight ids and reserves every purchased seat`() {
        assertTrue(seeder.seed())
        assertEquals(13, users.count())
        assertEquals(28, packages.count())
        assertEquals(26, purchases.count())
        assertEquals(19, reviews.count())
        assertEquals(15, favorites.count())
        assertTrue(packages.findAll().all { it.outboundFlightId >= 1001 })
        assertTrue(reviews.findAll().all { purchases.existsByBuyerIdAndTravelPackageId(it.buyer.id!!, it.travelPackage.id!!) })
        assertNotNull(runs.findById("ctv-demo-v1").orElseThrow().completedAt)
        val batch =
            Mockito
                .mockingDetails(flights)
                .invocations
                .single { it.method.name == "reserveDemoSeats" }
                .arguments[0] as List<*>
        assertEquals(52, batch.size)
        assertEquals(52, batch.distinct().size)
    }

    @Test
    fun `repeat seed preserves edits disabled users archived and deleted packages without new reservations`() {
        seeder.seed()
        val buyer = users.findByUsername("buyer")!!
        val hash = buyer.password
        buyer.enabled = false
        buyer.firstName = "Edited"
        users.saveAndFlush(buyer)
        val trip = packages.findAll().first { it.name == "París Romántico" }
        trip.active = false
        packages.saveAndFlush(trip)
        val removable = packages.findAll().first { it.name == "Dubái Express" }
        packages.deleteById(removable.id!!)
        assertFalse(seeder.seed())
        assertEquals(hash, users.findByUsername("buyer")!!.password)
        assertFalse(users.findByUsername("buyer")!!.enabled)
        assertEquals("Edited", users.findByUsername("buyer")!!.firstName)
        assertFalse(packages.findById(trip.id!!).orElseThrow().active)
        assertFalse(packages.existsById(removable.id!!))
        Mockito.verify(flights, Mockito.times(1)).seedCatalog()
        Mockito.verify(flights, Mockito.times(1)).reserveDemoSeats(Mockito.anyList())
    }

    @Test
    fun `first run preserves an existing demo username with a different role without adding buyer activity`() {
        userService.createAdmin("buyer", "custom123", "Existing", "Admin")
        seeder.seed()
        assertEquals(Role.ADMIN, users.findByUsername("buyer")!!.role)
        assertTrue(purchases.findAll().none { it.buyer.username == "buyer" })
        assertTrue(reviews.findAll().none { it.buyer.username == "buyer" })
        assertEquals("Existing", users.findByUsername("buyer")!!.firstName)
    }

    @Test
    fun `partial database is completed without skipping the entire hotel table or overwriting existing hotel`() {
        val paris = cities.save(City("PAR", "Paris"))
        val existing =
            hotels.save(
                Hotel
                    .Builder()
                    .name("Hotel Palacio de París")
                    .city(paris)
                    .photoUrl("https://example.com/custom.jpg")
                    .build(),
            )
        seeder.seed()
        assertEquals(30, hotels.count())
        assertEquals("https://example.com/custom.jpg", hotels.findById(existing.id!!).orElseThrow().photoUrl)
    }

    @Test
    fun `missing flights fail before writes and a remote reservation failure rolls back all local data`() {
        Mockito.`when`(flights.seedCatalog()).thenReturn(emptyList())
        assertThrows(IllegalArgumentException::class.java) { seeder.seed() }
        assertEquals(0, users.count())
        Mockito.`when`(flights.seedCatalog()).thenReturn(catalog())
        Mockito.doThrow(IllegalStateException("unavailable")).`when`(flights).reserveDemoSeats(Mockito.anyList())
        assertThrows(IllegalStateException::class.java) { seeder.seed() }
        assertEquals(0, users.count())
        assertEquals(0, packages.count())
        assertEquals(0, purchases.count())
        assertNull(runs.findById("ctv-demo-v1").orElseThrow().completedAt)
    }
}
