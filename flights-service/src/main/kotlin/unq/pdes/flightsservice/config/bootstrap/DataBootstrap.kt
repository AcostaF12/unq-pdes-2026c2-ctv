package unq.pdes.flightsservice.config.bootstrap

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import unq.pdes.flightsservice.model.Flight
import unq.pdes.flightsservice.persistence.jpa.FlightRepository
import unq.pdes.flightsservice.service.FlightService
import java.time.LocalDate
import java.time.LocalTime

/**
 * Seeds a deterministic flight catalog so the backend can reference flights by id.
 *
 * Convention (the backend bootstrap derives the very same ids, keep both in sync):
 * - Destinations, in order: [DESTINATIONS]. Origin is always BUE.
 * - Each destination has [VARIANTS] date variants (v = 0..2).
 * - For destination index i and variant v: outbound id = (i * 3 + v) * 2 + 1, return id = (i * 3 + v) * 2 + 2.
 *
 * Ids are guaranteed because flights are inserted in that exact order on an empty table.
 * In `dev` the tables are truncated with their identity restarted on every start, so the ids always match;
 * in `prod` nothing is touched when flights already exist.
 */
@Component
@Profile("dev", "prod")
class DataBootstrap(
    private val flightService: FlightService,
    private val flightRepository: FlightRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val environment: Environment,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        logger.info("Loading bootstrap data...")
        if (environment.activeProfiles.contains("dev")) {
            resetDevelopmentData()
        } else if (flightRepository.count() > 0) {
            logger.info("Flights: {}", flightRepository.count())
            return
        }
        loadFlights()
        logger.info("Flights: {}", flightRepository.count())
    }

    private fun resetDevelopmentData() {
        jdbcTemplate.execute("TRUNCATE TABLE flight_sales, flights RESTART IDENTITY CASCADE")
    }

    private fun loadFlights() {
        val today = LocalDate.now()
        DESTINATIONS.forEachIndexed { destinationIndex, destination ->
            VARIANTS.forEachIndexed { variantIndex, variant ->
                val outboundDate = today.plusDays(variant.daysAhead + destinationIndex % DATE_SPREAD_DAYS)
                val returnDate = outboundDate.plusDays(variant.stayDays)
                val airline = AIRLINES[(destinationIndex + variantIndex) % AIRLINES.size]
                val capacity = BASE_CAPACITY + CAPACITY_STEP * (destinationIndex % CAPACITY_TIERS)
                val (outboundSeats, returnSeats) = NEARLY_FULL[destinationIndex to variantIndex] ?: (capacity to capacity)
                flightService.save(flight(airline, outboundDate, variant.outboundTime, ORIGIN, destination, capacity, outboundSeats))
                flightService.save(flight(airline, returnDate, variant.returnTime, destination, ORIGIN, capacity, returnSeats))
            }
        }
    }

    private fun flight(
        airline: String,
        date: LocalDate,
        time: LocalTime,
        origin: String,
        destination: String,
        capacity: Int,
        availability: Int,
    ): Flight =
        Flight
            .Builder()
            .airline(airline)
            .flightDate(date)
            .departureTime(time)
            .origin(origin)
            .destination(destination)
            .capacity(capacity)
            .availability(availability)
            .build()

    private class DateVariant(
        val daysAhead: Long,
        val stayDays: Long,
        val outboundTime: LocalTime,
        val returnTime: LocalTime,
    )

    companion object {
        private const val ORIGIN = "BUE"
        private const val DATE_SPREAD_DAYS = 5
        private const val BASE_CAPACITY = 180
        private const val CAPACITY_STEP = 30
        private const val CAPACITY_TIERS = 3

        val DESTINATIONS = listOf("PAR", "LON", "ROM", "NYC", "RIO", "BCN", "CUN", "TYO", "DXB")

        private val VARIANTS =
            listOf(
                DateVariant(30, 7, LocalTime.of(8, 0), LocalTime.of(20, 30)),
                DateVariant(45, 10, LocalTime.of(11, 15), LocalTime.of(22, 40)),
                DateVariant(75, 14, LocalTime.of(6, 30), LocalTime.of(18, 10)),
            )

        private val AIRLINES =
            listOf("Aerolineas Argentinas", "LATAM Airlines", "Iberia", "Air France", "Emirates")

        /** (destination index, variant index) -> (outbound seats left, return seats left). */
        private val NEARLY_FULL =
            mapOf(
                (3 to 2) to (4 to 4),
                (4 to 1) to (8 to 12),
                (7 to 0) to (3 to 9),
                (8 to 2) to (2 to 6),
            )
    }
}
