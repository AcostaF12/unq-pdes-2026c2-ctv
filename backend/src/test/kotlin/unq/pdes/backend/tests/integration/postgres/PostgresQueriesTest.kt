package unq.pdes.backend.tests.integration.postgres

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import unq.pdes.backend.external.flights.ExternalFlightSaleDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.search.TravelPackageSearchCriteria
import unq.pdes.backend.service.PurchaseService
import unq.pdes.backend.service.TravelPackageService
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(
    properties = [
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.sql.init.mode=always",
        "spring.sql.init.platform=postgresql",
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
    fun `01 - flyway migrations match the entities and the postgres-only script runs`() {
        val trigramIndexes =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_packages_name_trgm'",
                Int::class.java,
            )

        assertEquals(1, trigramIndexes)
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

    private fun criteria(
        name: String? = null,
        origin: String? = null,
        destination: String? = null,
        minPrice: BigDecimal? = null,
        maxPrice: BigDecimal? = null,
    ) = TravelPackageSearchCriteria.from(name, origin, destination, minPrice, maxPrice)

    private fun pageByName() = PageRequest.of(0, 10, Sort.by("name"))
}
