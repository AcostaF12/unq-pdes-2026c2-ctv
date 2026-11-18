package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.AdminReportsDto
import unq.pdes.backend.service.AdminReportService

@Tag(name = "Administrative reports", description = "Solo ADMIN. Reportes históricos Top 5 calculados en la base local.")
@RestController
class AdminReportController(
    private val service: AdminReportService,
) {
    @Operation(
        summary = "Consultar los cuatro Top 5",
        description = "Compras, ventas, destinos vendidos y promedio por reseña. Sin filtros de fecha.",
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
