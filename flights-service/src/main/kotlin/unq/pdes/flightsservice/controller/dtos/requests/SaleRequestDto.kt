package unq.pdes.flightsservice.controller.dtos.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class SaleRequestDto(
    @field:Positive(message = "The sale must have a valid flight.")
    val flightId: Long = 0,
    @field:NotBlank(message = "The sale must have a passenger name.")
    val passengerName: String = "",
)
