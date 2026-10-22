package unq.pdes.flightsservice.config.bootstrap

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import unq.pdes.flightsservice.model.Flight
import unq.pdes.flightsservice.persistence.jpa.FlightRepository
import java.time.LocalDate
import java.time.LocalTime

/** Stable keys preserve dates, user-created flights, reservations and inventory across restarts. */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = ["enabled"], havingValue = "true")
class DataBootstrap(
    private val flightRepository: FlightRepository,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        val existing = flightRepository.findAll()
        val keyed = existing.mapNotNull { flight -> flight.seedKey?.let { it to flight } }.toMap()
        val changed = mutableListOf<Flight>()
        val today = LocalDate.now()
        DESTINATIONS.forEachIndexed { destinationIndex, destination ->
            VARIANTS.forEachIndexed { variantIndex, variant ->
                val date = today.plusDays(variant.daysAhead + destinationIndex % 5)
                val airline = AIRLINES[(destinationIndex + variantIndex) % AIRLINES.size]
                val capacity = 180 + 30 * (destinationIndex % 3)
                val (outboundSeats, returnSeats) = NEARLY_FULL[destinationIndex to variantIndex] ?: (capacity to capacity)
                val pair =
                    listOf(
                        flight(airline, date, variant.outboundTime, "BUE", destination, capacity, outboundSeats),
                        flight(airline, date.plusDays(variant.stayDays), variant.returnTime, destination, "BUE", capacity, returnSeats),
                    )
                pair.forEachIndexed { leg, candidate ->
                    val key = "ctv-demo-v1:$destination:$variantIndex:$leg"
                    if (key !in keyed) {
                        // Adopt an unambiguous legacy fixture without replacing stock or dates.
                        val legacy =
                            existing
                                .filter {
                                    it.seedKey == null &&
                                        it.origin == candidate.origin &&
                                        it.destination == candidate.destination &&
                                        it.airline == candidate.airline &&
                                        it.departureTime == candidate.departureTime
                                }.singleOrNull()
                        val resolved = legacy?.id?.let { flightRepository.findForUpdate(it) } ?: candidate
                        changed += resolved.apply { seedKey = key }
                    }
                }
            }
        }
        flightRepository.saveAll(changed)
        logger.info("Demo flights ready: {} added or adopted; existing inventory preserved.", changed.size)
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
        val DESTINATIONS = listOf("PAR", "LON", "ROM", "NYC", "RIO", "BCN", "CUN", "TYO", "DXB")
        private val VARIANTS =
            listOf(
                DateVariant(30, 7, LocalTime.of(8, 0), LocalTime.of(20, 30)),
                DateVariant(45, 10, LocalTime.of(11, 15), LocalTime.of(22, 40)),
                DateVariant(75, 14, LocalTime.of(6, 30), LocalTime.of(18, 10)),
            )
        private val AIRLINES = listOf("Aerolineas Argentinas", "LATAM Airlines", "Iberia", "Air France", "Emirates")
        private val NEARLY_FULL = mapOf((3 to 2) to (4 to 4), (4 to 1) to (8 to 12), (7 to 0) to (3 to 9), (8 to 2) to (2 to 6))
    }
}
