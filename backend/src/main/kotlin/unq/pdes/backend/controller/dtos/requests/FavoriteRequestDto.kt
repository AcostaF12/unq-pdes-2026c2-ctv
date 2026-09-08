package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.Positive

data class FavoriteRequestDto(
    @field:Positive(message = "The favorite must have a package.")
    val packageId: Long,
)
