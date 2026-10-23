package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Cambio de contraseña del usuario autenticado.")
data class ChangePasswordRequestDto(
    @field:Schema(description = "Contraseña actual.")
    @field:NotBlank(message = "The user must have a current password.")
    val currentPassword: String,
    @field:Schema(description = "Contraseña nueva, de al menos 6 caracteres.", example = "nueva123")
    @field:NotBlank(message = "The user must have a password.")
    @field:Size(min = 6, message = "The password must be at least 6 characters.")
    val newPassword: String,
)
