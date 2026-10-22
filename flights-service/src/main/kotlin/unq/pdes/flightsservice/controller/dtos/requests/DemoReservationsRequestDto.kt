package unq.pdes.flightsservice.controller.dtos.requests

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class DemoSeatRequestDto(
    @field:NotBlank @field:Size(max = 300) val key: String,
    @field:Positive val flightId: Long,
    @field:NotBlank @field:Size(max = 160) val passengerName: String,
)

data class DemoReservationsRequestDto(
    @field:Valid @field:Size(max = 200) val reservations: List<DemoSeatRequestDto>,
)
