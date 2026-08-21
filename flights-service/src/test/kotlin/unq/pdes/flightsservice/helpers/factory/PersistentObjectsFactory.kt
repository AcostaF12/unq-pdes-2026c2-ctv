package unq.pdes.flightsservice.helpers.factory

import org.springframework.stereotype.Component
import unq.pdes.flightsservice.model.Flight
import unq.pdes.flightsservice.service.FlightService
import java.time.LocalDate

@Component
class PersistentObjectsFactory(
    private val flightService: FlightService,
) : ObjectsFactory() {

    override fun anyFlight(): Flight {
        return flightService.save(super.anyFlight())
    }

    override fun flightFrom(origin: String, destination: String): Flight {
        return flightService.save(super.flightFrom(origin, destination))
    }

    override fun flightOn(date: LocalDate): Flight {
        return flightService.save(super.flightOn(date))
    }

    override fun flightWithAvailability(availability: Int): Flight {
        return flightService.save(super.flightWithAvailability(availability))
    }
}
