package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Credenciales para obtener un token.")
data class LoginRequestDto(
    @field:Schema(description = "Nombre de usuario.", example = "buyer")
    @field:NotBlank(message = "The user must have a username.")
    val username: String,
    @field:Schema(description = "Contraseña.", example = "buyer123")
    @field:NotBlank(message = "The user must have a password.")
    val password: String,
)
