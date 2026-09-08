package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank

data class HotelRequestDto(
    @field:NotBlank(message = "The hotel must have a name.")
    val name: String,
    @field:NotBlank(message = "The hotel must have a city.")
    val cityCode: String,
    @field:NotBlank(message = "The hotel must have a photo url.")
    val photoUrl: String,
)
