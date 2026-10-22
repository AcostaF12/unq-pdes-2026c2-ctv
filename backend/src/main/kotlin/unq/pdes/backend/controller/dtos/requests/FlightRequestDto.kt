package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
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
    @field:Schema(description = "Código IATA de una ciudad que ya exista, por ejemplo BUE.")
    val origin: String,
    @field:NotBlank(message = "The flight must have a destination.")
    @field:Schema(description = "Código IATA de una ciudad que ya exista, distinto del origen.")
    val destination: String,
    @field:Positive(message = "The flight capacity must be greater than zero.")
    val capacity: Int,
    @field:PositiveOrZero(message = "The flight availability cannot be negative.")
    val availability: Int,
)
