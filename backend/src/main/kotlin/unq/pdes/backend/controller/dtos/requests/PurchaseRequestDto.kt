package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.Positive

data class PurchaseRequestDto(
    @field:Positive(message = "The purchase must have a package.")
    val packageId: Long,
)
