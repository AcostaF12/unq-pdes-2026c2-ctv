package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Alta de un comprador. No crea agencias ni administradores.")
data class RegisterRequestDto(
    @field:Schema(description = "Nombre de usuario, único.", example = "nuevo.comprador")
    @field:NotBlank(message = "The user must have a username.")
    val username: String,
    @field:Schema(description = "Contraseña.", example = "secreto123")
    @field:NotBlank(message = "The user must have a password.")
    val password: String,
    @field:Schema(description = "Nombre.", example = "Ana")
    @field:NotBlank(message = "The user must have a first name.")
    val firstName: String,
    @field:Schema(description = "Apellido.", example = "Pérez")
    @field:NotBlank(message = "The user must have a last name.")
    val lastName: String,
)
