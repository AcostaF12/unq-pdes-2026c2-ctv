package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.AdminReportsDto
import unq.pdes.backend.service.AdminReportService

@Tag(
    name = "Administrative reports",
    description = "Cuatro rankings de como máximo cinco filas, calculados en la base del negocio. Solo admin. No llama a vuelos.",
)
@RestController
class AdminReportController(
    private val service: AdminReportService,
) {
    @Operation(
        summary = "Consultar los cuatro Top 5",
        description =
            "Trae cuatro rankings de hasta cinco filas: compradores, agencias, destinos vendidos y destinos mejor puntuados.\n\n" +
                "Lo vendido sale de la copia de cada compra. El puntaje usa el destino actual del paquete. No filtra por fecha.\n\n" +
                "**Quién:** admin",
    )
    @GetMapping("/admin/reports")
    fun reports(): AdminReportsDto =
        AdminReportsDto(
            service.topBuyers(),
            service.topAgencies(),
            service.topDestinations(),
            service.topRatedDestinations(),
        )
}
