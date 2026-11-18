package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Nombre visible del usuario autenticado. No cambia el usuario ni el rol.")
data class UpdateProfileRequestDto(
    @field:Schema(description = "Nombre. Hasta 80 caracteres.", example = "Ana")
    @field:NotBlank(message = "The user must have a first name.")
    @field:Size(max = 80, message = "The first name must be at most 80 characters.")
    val firstName: String,
    @field:Schema(description = "Apellido. Hasta 80 caracteres.", example = "Pérez")
    @field:NotBlank(message = "The user must have a last name.")
    @field:Size(max = 80, message = "The last name must be at most 80 characters.")
    val lastName: String,
)
