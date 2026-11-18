package unq.pdes.flightsservice.tests.integration.postgres

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import unq.pdes.flightsservice.model.Flight
import unq.pdes.flightsservice.persistence.jpa.FlightRepository
import unq.pdes.flightsservice.service.FlightService
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(
    properties = [
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create",
    ],
)
@Transactional
class PostgresFlightSearchTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }

    @Autowired
    private lateinit var flightService: FlightService

    @Autowired
    private lateinit var flightRepository: FlightRepository

    @BeforeEach
    fun setUp() {
        saveFlight("Paris", "BUE", "PAR", "2026-12-01", "08:00")
        saveFlight("Later", "BUE", "PAR", "2026-12-02", "08:00")
        saveFlight("Madrid", "MAD", "PAR", "2026-12-01", "06:00")
        saveFlight("Roma", "BUE", "ROM", "2026-12-01", "07:00")
        saveFlight("Return", "PAR", "BUE", "2026-12-10", "08:00")
        saveFlight("Sold out", "BUE", "PAR", "2026-12-01", "05:00", availability = 0)
        flightRepository.flush()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "none, none, none, Madrid|Roma|Paris|Later|Return",
            "BUE, none, none, Roma|Paris|Later",
            "none, PAR, none, Madrid|Paris|Later",
            "none, none, 2026-12-01, Madrid|Roma|Paris",
            "BUE, PAR, none, Paris|Later",
            "BUE, none, 2026-12-01, Roma|Paris",
            "none, PAR, 2026-12-01, Madrid|Paris",
            "BUE, PAR, 2026-12-01, Paris",
        ],
        nullValues = ["none"],
    )
    fun `optional filters work on postgres and exclude sold out flights`(
        origin: String?,
        destination: String?,
        date: String?,
        expectedAirlines: String,
    ) {
        // Repeat past the JDBC prepared-statement threshold used in production.
        repeat(6) {
            val result = flightService.search(origin, destination, date?.let(LocalDate::parse))

            assertEquals(expectedAirlines, result.joinToString("|") { it.airline })
        }
    }

    @Test
    fun `searching a date without flights returns an empty list`() {
        assertEquals(emptyList<Flight>(), flightService.search("BUE", "PAR", LocalDate.of(2026, 12, 30)))
    }

    private fun saveFlight(
        airline: String,
        origin: String,
        destination: String,
        date: String,
        time: String,
        availability: Int = 5,
    ) {
        flightRepository.save(
            Flight
                .Builder()
                .airline(airline)
                .origin(origin)
                .destination(destination)
                .flightDate(LocalDate.parse(date))
                .departureTime(LocalTime.parse(time))
                .capacity(5)
                .availability(availability)
                .build(),
        )
    }
}
