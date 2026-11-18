package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

@Schema(description = "Paquete que el comprador quiere comprar. No se mandan los vuelos: salen del paquete.")
data class PurchaseRequestDto(
    @field:Schema(description = "Id del paquete publicado.", example = "1")
    @field:Positive(message = "The purchase must have a package.")
    val packageId: Long,
)
