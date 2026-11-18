package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Agencia nueva y el vendedor que la administra. Se crean juntos.")
data class CreateAgencyRequestDto(
    @field:Schema(description = "Nombre de la agencia.", example = "Andes Travel")
    @field:NotBlank
    @field:Size(max = 160)
    val name: String,
    @field:Schema(description = "Usuario del vendedor. Único.", example = "andes")
    @field:NotBlank
    @field:Size(max = 60)
    val username: String,
    @field:Schema(description = "Contraseña del vendedor, de 6 a 72 caracteres.", example = "andes123")
    @field:NotBlank
    @field:Size(min = 6, max = 72)
    val password: String,
    @field:Schema(description = "Nombre del vendedor.", example = "Luis")
    @field:NotBlank
    @field:Size(max = 80)
    val firstName: String,
    @field:Schema(description = "Apellido del vendedor.", example = "Gómez")
    @field:NotBlank
    @field:Size(max = 80)
    val lastName: String,
)
