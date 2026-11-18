package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

@Schema(description = "Oferta de una agencia. La vuelta sale del destino de la ida y el hotel está en esa ciudad.")
data class TravelPackageRequestDto(
    @field:Schema(description = "Nombre único dentro de la agencia.", example = "Bariloche 5 noches")
    @field:NotBlank(message = "The package must have a name.")
    val name: String,
    @field:Schema(description = "Id del hotel, en la ciudad de destino.", example = "1")
    @field:Positive(message = "The package must have a hotel.")
    val hotelId: Long,
    @field:Schema(description = "Id del vuelo de ida, del servicio de vuelos.", example = "10")
    @field:Positive(message = "The package must have an outbound flight.")
    val outboundFlightId: Long,
    @field:Schema(description = "Id del vuelo de vuelta. Su origen es el destino de la ida.", example = "11")
    @field:Positive(message = "The package must have a return flight.")
    val returnFlightId: Long,
    @field:Schema(description = "Precio que se congela en la compra.", example = "250000.00")
    @field:Positive(message = "The package price must be greater than zero.")
    val price: BigDecimal,
    @field:Schema(description = "Obligatorio si llama un admin. La agencia lo ignora y usa la propia.")
    val agencyId: Long? = null,
)
