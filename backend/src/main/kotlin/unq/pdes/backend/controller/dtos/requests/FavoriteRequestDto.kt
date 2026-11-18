package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

@Schema(description = "Paquete a marcar. No reserva un asiento.")
data class FavoriteRequestDto(
    @field:Schema(description = "Id del paquete.", example = "1")
    @field:Positive(message = "The favorite must have a package.")
    val packageId: Long,
)
