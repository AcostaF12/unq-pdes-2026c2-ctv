package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank

data class RegisterRequestDto(
    @field:NotBlank(message = "The user must have a username.")
    val username: String,
    @field:NotBlank(message = "The user must have a password.")
    val password: String,
    @field:NotBlank(message = "The user must have a first name.")
    val firstName: String,
    @field:NotBlank(message = "The user must have a last name.")
    val lastName: String,
)
