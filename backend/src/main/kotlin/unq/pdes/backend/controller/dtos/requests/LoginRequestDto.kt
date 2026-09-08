package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank

data class LoginRequestDto(
    @field:NotBlank(message = "The user must have a username.")
    val username: String,
    @field:NotBlank(message = "The user must have a password.")
    val password: String,
)
