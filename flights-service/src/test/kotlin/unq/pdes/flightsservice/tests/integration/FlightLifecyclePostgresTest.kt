package unq.pdes.flightsservice.tests.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import unq.pdes.flightsservice.config.bootstrap.DataBootstrap
import unq.pdes.flightsservice.model.DemoSeatReservation
import unq.pdes.flightsservice.persistence.jpa.FlightRepository
import unq.pdes.flightsservice.persistence.jpa.FlightSaleRepository
import unq.pdes.flightsservice.service.DemoReservationService
import unq.pdes.flightsservice.service.FlightService
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(
    properties = [
        "app.seed.enabled=true", "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class FlightLifecyclePostgresTest {
    @Autowired private lateinit var context: WebApplicationContext

    companion object {
        @Container @ServiceConnection @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }

    @Autowired private lateinit var jdbc: JdbcTemplate

    @Autowired private lateinit var bootstrap: DataBootstrap

    @Autowired private lateinit var flights: FlightRepository

    @Autowired private lateinit var sales: FlightSaleRepository

    @Autowired private lateinit var service: FlightService

    @Autowired private lateinit var reservations: DemoReservationService

    @BeforeEach
    fun clean() = jdbc.execute("TRUNCATE flight_sales, flights RESTART IDENTITY CASCADE")

    private fun custom(seats: Int = 2) = service.create("Custom", LocalDate.now().plusDays(100), LocalTime.NOON, "BUE", "PAR", seats, seats)

    @Test
    fun `flyway validates real postgres and the seed preserves custom flights dates and sold seats across restarts`() {
        val custom = custom()
        bootstrap.run(DefaultApplicationArguments())
        val catalog = service.seeded()
        assertEquals(54, catalog.size)
        assertTrue(catalog.all { it.id != custom.id })
        val first = catalog.first()
        val request = DemoSeatReservation("ctv-demo-v1:buyer:trip:0", first.id!!, "Buyer Demo")
        reservations.reserve(listOf(request))
        val remaining = service.findById(first.id!!).availability
        bootstrap.run(DefaultApplicationArguments())
        reservations.reserve(listOf(request))
        assertEquals(55, flights.count())
        assertEquals(1, sales.count())
        assertEquals(remaining, service.findById(first.id!!).availability)
        assertEquals(first.flightDate, service.findById(first.id!!).flightDate)
        assertEquals("Custom", service.findById(custom.id!!).airline)
    }

    @Test
    fun `demo HTTP contract validates nested requests before reserving seats`() {
        bootstrap.run(DefaultApplicationArguments())
        val flight = custom()
        val mvc = MockMvcBuilders.webAppContextSetup(context).build()
        mvc
            .perform(get("/internal/demo/flights"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].seedKey").isNotEmpty)
        val body = """{"reservations":[{"key":"ctv-demo-v1:http","flightId":${flight.id},"passengerName":"Demo Buyer"}]}"""
        repeat(2) {
            mvc
                .perform(post("/internal/demo/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent)
        }
        mvc
            .perform(
                post("/internal/demo/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.replace("Demo Buyer", "")),
            ).andExpect(status().isBadRequest)
        assertEquals(1, service.findById(flight.id!!).availability)
        assertEquals(1, sales.count())
    }

    @Test
    fun `legacy seed flight is adopted without resetting its stock or changing its real id`() {
        val legacy = service.create("Aerolineas Argentinas", LocalDate.now().plusDays(20), LocalTime.of(8, 0), "BUE", "PAR", 180, 177)
        bootstrap.run(DefaultApplicationArguments())
        val adopted = service.seeded().single { it.seedKey == "ctv-demo-v1:PAR:0:0" }
        assertEquals(legacy.id, adopted.id)
        assertEquals(177, adopted.availability)
        assertEquals(legacy.flightDate, adopted.flightDate)
        assertEquals(54, flights.count())
    }

    @Test
    fun `demo reservation batch rolls back on insufficient inventory and rejects reuse with a different flight`() {
        val first = custom(1)
        val second = custom(1)
        val request = DemoSeatReservation("ctv-demo-v1:buyer:trip:0", first.id!!, "Buyer Demo")
        assertThrows(IllegalArgumentException::class.java) {
            reservations.reserve(listOf(request, request.copy(key = "ctv-demo-v1:other")))
        }
        assertEquals(1, service.findById(first.id!!).availability)
        assertEquals(0, sales.count())
        reservations.reserve(listOf(request))
        assertThrows(IllegalArgumentException::class.java) { reservations.reserve(listOf(request.copy(flightId = second.id!!))) }
        assertEquals(1, service.findById(second.id!!).availability)
    }

    @Test
    fun `two simultaneous buyers cannot sell the same last seat`() {
        val flight = custom(1)
        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        try {
            val futures =
                (1..2).map { index ->
                    executor.submit(
                        Callable {
                            ready.countDown()
                            check(start.await(10, TimeUnit.SECONDS))
                            runCatching { service.sell(flight.id!!, "Buyer $index") }.isSuccess
                        },
                    )
                }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            assertEquals(1, futures.count { it.get(15, TimeUnit.SECONDS) })
            assertEquals(0, service.findById(flight.id!!).availability)
            assertEquals(1, sales.count())
        } finally {
            executor.shutdownNow()
        }
    }
}
