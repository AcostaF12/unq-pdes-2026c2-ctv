package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive

@Schema(description = "Opinión de un paquete ya comprado. Una sola por comprador.")
data class ReviewRequestDto(
    @field:Schema(description = "Id del paquete comprado.", example = "1")
    @field:Positive(message = "The review must have a package.")
    val packageId: Long,
    @field:Schema(description = "Puntaje de 0 a 10.", example = "8")
    @field:Min(value = 0, message = "The score must be between 0 and 10.")
    @field:Max(value = 10, message = "The score must be between 0 and 10.")
    val score: Int,
    @field:Schema(description = "Comentario opcional.", example = "El hotel estaba muy bien.")
    val comment: String? = null,
)
