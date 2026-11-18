package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Publica o archiva un paquete sin borrar su historial.")
data class PackageStatusRequestDto(
    @field:Schema(description = "true lo publica de nuevo. false lo archiva y rechaza compras nuevas.", example = "false")
    @field:NotNull val active: Boolean?,
)
