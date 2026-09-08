package unq.pdes.flightsservice.controller.dtos.requests

import jakarta.validation.constraints.NotBlank

data class FlightSaleRequestDto(
    @field:NotBlank(message = "The sale must have a passenger name.")
    val passengerName: String,
)
