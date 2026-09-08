package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateProfileRequestDto(
    @field:NotBlank(message = "The user must have a first name.")
    @field:Size(max = 80, message = "The first name must be at most 80 characters.")
    val firstName: String,
    @field:NotBlank(message = "The user must have a last name.")
    @field:Size(max = 80, message = "The last name must be at most 80 characters.")
    val lastName: String,
)
