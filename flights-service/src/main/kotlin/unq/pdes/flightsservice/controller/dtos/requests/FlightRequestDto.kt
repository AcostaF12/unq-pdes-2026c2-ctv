package unq.pdes.flightsservice.controller.dtos.requests

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDate
import java.time.LocalTime

data class FlightRequestDto(
    @field:NotBlank(message = "The flight must have an airline.")
    val airline: String,
    @field:NotNull(message = "The flight must have a date.")
    val flightDate: LocalDate,
    @field:NotNull(message = "The flight must have a departure time.")
    val departureTime: LocalTime,
    @field:NotBlank(message = "The flight must have an origin.")
    val origin: String,
    @field:NotBlank(message = "The flight must have a destination.")
    val destination: String,
    @field:Positive(message = "The flight capacity must be greater than zero.")
    val capacity: Int,
    @field:Min(value = 0, message = "The flight availability cannot be negative.")
    val availability: Int,
)
