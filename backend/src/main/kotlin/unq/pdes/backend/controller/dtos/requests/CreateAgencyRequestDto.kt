package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAgencyRequestDto(
    @field:NotBlank @field:Size(max = 160) val name: String,
    @field:NotBlank @field:Size(max = 60) val username: String,
    @field:NotBlank @field:Size(min = 6, max = 72) val password: String,
    @field:NotBlank @field:Size(max = 80) val firstName: String,
    @field:NotBlank @field:Size(max = 80) val lastName: String,
)
