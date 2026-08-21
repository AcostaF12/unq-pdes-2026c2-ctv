package unq.pdes.flightsservice.config.bootstrap

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import unq.pdes.flightsservice.model.Flight
import unq.pdes.flightsservice.persistence.jpa.FlightRepository
import unq.pdes.flightsservice.service.FlightService
import java.time.LocalDate
import java.time.LocalTime

@Component
@Profile("dev", "prod")
class DataBootstrap(
    private val flightService: FlightService,
    private val flightRepository: FlightRepository,
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        logger.info("Loading bootstrap data...")
        if (flightRepository.count() > 0) {
            logger.info("Flights: {}", flightRepository.count())
            return
        }
        loadFlights()
        logger.info("Flights: {}", flightRepository.count())
    }

    private fun loadFlights() {
        val baseDate = LocalDate.now().plusDays(30)
        val destinations = listOf("PAR", "LON", "ROM", "NYC", "RIO", "BCN", "CUN")

        destinations.forEachIndexed { index, destination ->
            val outboundDate = baseDate.plusDays(index.toLong())
            val returnDate = outboundDate.plusDays(7)
            flightService.save(
                flight("Aerolineas Argentinas", outboundDate, LocalTime.of(8, 0), "BUE", destination, 180, 180),
            )
            flightService.save(
                flight("Aerolineas Argentinas", returnDate, LocalTime.of(20, 30), destination, "BUE", 180, 180),
            )
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
    ): Flight {
        return Flight.Builder()
            .airline(airline)
            .flightDate(date)
            .departureTime(time)
            .origin(origin)
            .destination(destination)
            .capacity(capacity)
            .availability(availability)
            .build()
    }
}
