package unq.pdes.flightsservice.controller.dtos.models

import unq.pdes.flightsservice.model.Flight
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
        fun fromModel(flight: Flight): FlightDto {
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

    fun toModel(): Flight {
        return Flight.Builder()
            .id(id)
            .airline(airline)
            .flightDate(flightDate)
            .departureTime(departureTime)
            .origin(origin)
            .destination(destination)
            .capacity(capacity)
            .availability(availability)
            .build()
    }
}
