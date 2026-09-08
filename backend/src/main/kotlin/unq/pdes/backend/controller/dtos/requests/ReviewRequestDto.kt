package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive

data class ReviewRequestDto(
    @field:Positive(message = "The review must have a package.")
    val packageId: Long,
    @field:Min(value = 0, message = "The score must be between 0 and 10.")
    @field:Max(value = 10, message = "The score must be between 0 and 10.")
    val score: Int,
    val comment: String? = null,
)
