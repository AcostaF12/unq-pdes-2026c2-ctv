package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class HotelRequestDto(
    @field:NotBlank(message = "The hotel must have a name.")
    val name: String,
    @field:NotBlank(message = "The hotel must have a city.")
    @field:Schema(description = "Código IATA de una ciudad que ya exista, por ejemplo BUE.")
    val cityCode: String,
    @field:NotBlank(message = "The hotel must have a photo url.")
    val photoUrl: String,
)
