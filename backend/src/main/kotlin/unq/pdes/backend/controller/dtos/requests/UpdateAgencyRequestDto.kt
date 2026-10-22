package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UpdateAgencyRequestDto(
    @field:NotBlank @field:Size(max = 160) val name: String,
    @field:NotNull val active: Boolean?,
)
