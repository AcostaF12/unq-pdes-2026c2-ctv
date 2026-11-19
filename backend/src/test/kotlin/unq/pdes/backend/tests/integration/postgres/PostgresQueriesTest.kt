package unq.pdes.backend.tests.integration.postgres

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import unq.pdes.backend.external.flights.ExternalFlightSaleDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.search.TravelPackageSearchCriteria
import unq.pdes.backend.service.PurchaseService
import unq.pdes.backend.service.TravelPackageService
import unq.pdes.backend.service.UserAdministrationService
import unq.pdes.backend.service.UserService
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(
    properties = [
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    ],
)
class PostgresQueriesTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var travelPackageService: TravelPackageService

    @Autowired
    private lateinit var purchaseService: PurchaseService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var administration: UserAdministrationService

    @Autowired
    private lateinit var users: UserService

    @MockitoBean
    private lateinit var flightsClient: FlightsClient

    @BeforeEach
    fun setUp() {
        Mockito
            .`when`(flightsClient.sell(1L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(10L, 1L, "Bruno Buyer"))
        Mockito
            .`when`(flightsClient.sell(2L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(11L, 2L, "Bruno Buyer"))
    }

    @AfterEach
    fun tearDown() {
        val tables =
            jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'",
                String::class.java,
            )
        jdbcTemplate.execute("TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE")
    }

    @Test
    fun `01 - flyway runs the common and the postgres-only migrations and creates every index`() {
        val indexes =
            jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'packages'",
                String::class.java,
            )
        assertTrue(
            indexes.containsAll(
                listOf(
                    "idx_packages_price",
                    "idx_packages_route_price",
                    "idx_packages_destination_price",
                    "idx_packages_active_destination",
                    "idx_packages_name_trgm",
                ),
            ),
            "missing package indexes, found: $indexes",
        )
        val appliedVersions =
            jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String::class.java,
            )
        assertTrue(appliedVersions.containsAll(listOf("7", "7.1")), "applied versions: $appliedVersions")
    }

    @Test
    fun `01b - the package name filter can be served by the trigram index`() {
        val plan =
            jdbcTemplate.execute(
                ConnectionCallback { connection ->
                    connection.createStatement().use { statement ->
                        // Tiny test tables always favour a sequential scan; disabling it shows the index is usable.
                        statement.execute("SET enable_seqscan = off")
                        try {
                            statement.executeQuery("EXPLAIN SELECT id FROM packages WHERE lower(name) LIKE '%par%'").use { rows ->
                                generateSequence { if (rows.next()) rows.getString(1) else null }.joinToString(" | ")
                            }
                        } finally {
                            statement.execute("RESET enable_seqscan")
                        }
                    }
                },
            )!!
        assertTrue(plan.contains("idx_packages_name_trgm"), plan)
    }

    @Test
    fun `01c - purchase city name snapshots are as wide as cities name`() {
        val lengths =
            jdbcTemplate.queryForList(
                "SELECT character_maximum_length FROM information_schema.columns WHERE table_name = 'purchases' " +
                    "AND column_name IN ('hotel_snapshot_city_name', 'origin_snapshot_name', 'destination_snapshot_name')",
                Int::class.java,
            )
        assertEquals(listOf(255, 255, 255), lengths)
    }

    @Test
    fun `02 - searching packages without filters returns every package`() {
        factory.packageNamed("París Romántico")
        factory.packageNamed("Roma Imperial", destinationCode = "ROM", destinationCity = "Rome")

        val result = travelPackageService.search(criteria(), pageByName())

        assertEquals(2, result.totalElements)
    }

    @Test
    fun `03 - searching packages by name ignores case and matches part of the name`() {
        factory.packageNamed("París Romántico")
        factory.packageNamed("Roma Imperial", destinationCode = "ROM", destinationCity = "Rome")

        assertEquals(1, travelPackageService.search(criteria(name = "PAR"), pageByName()).totalElements)
        assertEquals(1, travelPackageService.search(criteria(name = "imperial"), pageByName()).totalElements)
        assertEquals(0, travelPackageService.search(criteria(name = "zzz"), pageByName()).totalElements)
    }

    @Test
    fun `04 - searching packages by destination and price range`() {
        factory.packageNamed("París Romántico", price = BigDecimal("1500.00"))
        factory.packageNamed("Roma Imperial", destinationCode = "ROM", destinationCity = "Rome", price = BigDecimal("1650.00"))

        assertEquals(1, travelPackageService.search(criteria(destination = "rom"), pageByName()).totalElements)
        assertEquals(1, travelPackageService.search(criteria(minPrice = BigDecimal("1600")), pageByName()).totalElements)
        assertEquals(2, travelPackageService.search(criteria(maxPrice = BigDecimal("1650")), pageByName()).totalElements)
        assertEquals(0, travelPackageService.search(criteria(minPrice = BigDecimal("2000")), pageByName()).totalElements)
    }

    @Test
    fun `05 - agency history without filters lists the sales of the agency`() {
        val agencyUser = factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        purchaseService.purchase(buyer.username, factory.packageNamed("París Romántico").id!!)

        val history = purchaseService.findForAgency(agencyUser.username)

        assertEquals(1, history.totalElements)
    }

    @Test
    fun `06 - agency history filters by package name, buyer and dates`() {
        val agencyUser = factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        purchaseService.purchase(buyer.username, factory.packageNamed("París Romántico").id!!)
        val today = LocalDate.now()

        fun count(
            packageName: String? = null,
            buyerUsername: String? = null,
            from: LocalDate? = null,
            to: LocalDate? = null,
        ) = purchaseService
            .findForAgency(agencyUser.username, buyerUsername = buyerUsername, packageName = packageName, from = from, to = to)
            .totalElements

        assertEquals(1, count(packageName = "PAR"))
        assertEquals(0, count(packageName = "zzz"))
        assertEquals(1, count(buyerUsername = "buyer"))
        assertEquals(0, count(buyerUsername = "nadie"))
        assertEquals(1, count(from = today.minusDays(1), to = today.plusDays(1)))
        assertEquals(0, count(from = today.plusDays(1)))
        assertEquals(0, count(to = today.minusDays(1)))
    }

    @Test
    fun `07 - account role transitions respect postgres constraints and preserve purchase history`() {
        factory.adminNamed("admin")
        val buyer = factory.buyerNamed("buyer")
        val trip = factory.packageNamed("Historical trip")
        val purchase = purchaseService.purchase("buyer", trip.id!!)
        administration.update("admin", buyer.id!!, "Ana", "Pérez", Role.AGENCY, true, trip.agency.id)
        assertTrue(users.findByUsername("buyer") is AgencyUser)
        administration.update("admin", buyer.id!!, "Ana", "Pérez", Role.ADMIN, false, null)
        val updated = users.findByUsername("buyer")
        assertEquals(Role.ADMIN, updated.role)
        assertEquals(false, updated.enabled)
        assertEquals(buyer.id, updated.id)
        assertEquals(purchase.id, purchaseService.findForViewer("admin", purchase.id!!).id)
        val sales = purchaseService.findForAdmin("admin", 0, 20, null, null, null, null)
        assertEquals(1, sales.totalElements)
        assertEquals(
            "Bruno",
            sales.content
                .first()
                .buyerSnapshot.firstName,
        )
    }

    private fun criteria(
        name: String? = null,
        origin: String? = null,
        destination: String? = null,
        minPrice: BigDecimal? = null,
        maxPrice: BigDecimal? = null,
    ) = TravelPackageSearchCriteria.from(name, origin, destination, minPrice, maxPrice)

    private fun pageByName() = PageRequest.of(0, 10, Sort.by("name"))
}
