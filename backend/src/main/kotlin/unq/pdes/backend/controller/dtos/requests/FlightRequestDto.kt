package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "Vuelo que se crea en el servicio de vuelos. El id de la respuesta se usa en un paquete.")
data class FlightRequestDto(
    @field:Schema(description = "Aerolínea.", example = "Aerolíneas Argentinas")
    @field:NotBlank(message = "The flight must have an airline.")
    val airline: String,
    @field:Schema(description = "Fecha del vuelo.", example = "2026-12-01")
    val flightDate: LocalDate,
    @field:Schema(description = "Hora de salida.", example = "08:30:00")
    val departureTime: LocalTime,
    @field:Schema(description = "Código de ciudad de origen.", example = "BUE")
    @field:NotBlank(message = "The flight must have an origin.")
    val origin: String,
    @field:Schema(description = "Código de ciudad de destino.", example = "PAR")
    @field:NotBlank(message = "The flight must have a destination.")
    val destination: String,
    @field:Schema(description = "Asientos totales, mayor que cero.", example = "180")
    @field:Positive(message = "The flight capacity must be greater than zero.")
    val capacity: Int,
    @field:Schema(description = "Lugares libres. Cero o más, y no puede superar la capacidad.", example = "180")
    @field:PositiveOrZero(message = "The flight availability cannot be negative.")
    val availability: Int,
)
