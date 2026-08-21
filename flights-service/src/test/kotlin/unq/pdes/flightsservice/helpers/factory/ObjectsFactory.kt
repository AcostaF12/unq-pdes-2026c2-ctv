package unq.pdes.flightsservice.helpers.factory

import unq.pdes.flightsservice.model.Flight
import java.time.LocalDate
import java.time.LocalTime

open class ObjectsFactory {

    open fun anyFlight(): Flight {
        return baseBuilder().build()
    }

    open fun flightFrom(origin: String, destination: String): Flight {
        return baseBuilder().origin(origin).destination(destination).build()
    }

    open fun flightOn(date: LocalDate): Flight {
        return baseBuilder().flightDate(date).build()
    }

    open fun flightWithAvailability(availability: Int): Flight {
        return baseBuilder().capacity(200).availability(availability).build()
    }

    private fun baseBuilder(): Flight.Builder {
        return Flight.Builder()
            .airline("Aerolineas Argentinas")
            .flightDate(DEFAULT_DATE)
            .departureTime(LocalTime.of(8, 0))
            .origin("BUE")
            .destination("PAR")
            .capacity(180)
            .availability(180)
    }

    companion object {
        val DEFAULT_DATE: LocalDate = LocalDate.of(2026, 12, 1)
    }
}
