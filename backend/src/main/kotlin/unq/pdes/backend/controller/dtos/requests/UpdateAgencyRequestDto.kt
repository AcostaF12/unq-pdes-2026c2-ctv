package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "Nombre y estado de una agencia. Desactivarla no borra paquetes ni compras.")
data class UpdateAgencyRequestDto(
    @field:Schema(description = "Nombre de la agencia.", example = "Andes Travel")
    @field:NotBlank
    @field:Size(max = 160)
    val name: String,
    @field:Schema(description = "false bloquea a sus vendedores y rechaza compras nuevas.", example = "true")
    @field:NotNull
    val active: Boolean?,
)
