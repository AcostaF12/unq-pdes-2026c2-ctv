package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Datos de un hotel. La ciudad tiene que existir.")
data class HotelRequestDto(
    @field:Schema(description = "Nombre del hotel.", example = "Hotel Austral")
    @field:NotBlank(message = "The hotel must have a name.")
    val name: String,
    @field:Schema(description = "Código de ciudad de GET /cities.", example = "PAR")
    @field:NotBlank(message = "The hotel must have a city.")
    val cityCode: String,
    @field:Schema(description = "URL de la foto.", example = "https://example.com/hotel.jpg")
    @field:NotBlank(message = "The hotel must have a photo url.")
    val photoUrl: String,
)
