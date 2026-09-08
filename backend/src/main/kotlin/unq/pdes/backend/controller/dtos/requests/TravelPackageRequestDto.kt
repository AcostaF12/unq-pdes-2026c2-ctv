package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class TravelPackageRequestDto(
    @field:NotBlank(message = "The package must have a name.")
    val name: String,
    @field:Positive(message = "The package must have a hotel.")
    val hotelId: Long,
    @field:Positive(message = "The package must have an outbound flight.")
    val outboundFlightId: Long,
    @field:Positive(message = "The package must have a return flight.")
    val returnFlightId: Long,
    @field:Positive(message = "The package price must be greater than zero.")
    val price: BigDecimal,
)
