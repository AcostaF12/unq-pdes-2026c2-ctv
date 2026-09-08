package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequestDto(
    @field:NotBlank(message = "The user must have a current password.")
    val currentPassword: String,
    @field:NotBlank(message = "The user must have a password.")
    @field:Size(min = 6, message = "The password must be at least 6 characters.")
    val newPassword: String,
)
