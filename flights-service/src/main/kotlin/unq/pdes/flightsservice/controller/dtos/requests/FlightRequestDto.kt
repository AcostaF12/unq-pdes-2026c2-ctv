package unq.pdes.flightsservice.controller.dtos.requests

import java.time.LocalDate
import java.time.LocalTime

data class FlightRequestDto(
    val airline: String,
    val flightDate: LocalDate,
    val departureTime: LocalTime,
    val origin: String,
    val destination: String,
    val capacity: Int,
    val availability: Int,
)
