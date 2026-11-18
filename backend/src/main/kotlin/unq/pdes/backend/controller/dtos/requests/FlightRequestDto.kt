package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate
import java.time.LocalTime

data class FlightRequestDto(
    @field:NotBlank(message = "The flight must have an airline.")
    val airline: String,
    val flightDate: LocalDate,
    val departureTime: LocalTime,
    @field:NotBlank(message = "The flight must have an origin.")
    val origin: String,
    @field:NotBlank(message = "The flight must have a destination.")
    val destination: String,
    @field:Positive(message = "The flight capacity must be greater than zero.")
    val capacity: Int,
    @field:PositiveOrZero(message = "The flight availability cannot be negative.")
    val availability: Int,
)
