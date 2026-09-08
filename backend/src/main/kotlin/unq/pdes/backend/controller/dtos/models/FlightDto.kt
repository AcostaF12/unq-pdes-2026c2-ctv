package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.external.flights.ExternalFlightDto
import java.time.LocalDate
import java.time.LocalTime

data class FlightDto(
    val id: Long?,
    val airline: String,
    val flightDate: LocalDate,
    val departureTime: LocalTime,
    val origin: String,
    val destination: String,
    val capacity: Int,
    val availability: Int,
) {
    companion object {
        fun fromExternal(flight: ExternalFlightDto): FlightDto {
            return FlightDto(
                id = flight.id,
                airline = flight.airline,
                flightDate = flight.flightDate,
                departureTime = flight.departureTime,
                origin = flight.origin,
                destination = flight.destination,
                capacity = flight.capacity,
                availability = flight.availability,
            )
        }
    }
}
