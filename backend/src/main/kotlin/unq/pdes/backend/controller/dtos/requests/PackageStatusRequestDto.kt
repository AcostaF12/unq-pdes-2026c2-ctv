package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotNull

data class PackageStatusRequestDto(
    @field:NotNull val active: Boolean?,
)
