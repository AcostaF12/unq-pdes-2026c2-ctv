package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.PurchaseHistoryPageDto
import unq.pdes.backend.service.PurchaseService
import java.time.LocalDate

@Tag(name = "Administración de ventas")
@RestController
@RequestMapping("/admin/sales")
@PreAuthorize("hasRole('ADMIN')")
class AdminSalesController(
    private val purchaseService: PurchaseService,
) {
    @Operation(summary = "Ventas de todas las agencias", description = "Historial paginado con filtros de comprador, paquete y fechas.")
    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) buyerUsername: String?,
        @RequestParam(required = false) packageName: String?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): PurchaseHistoryPageDto =
        PurchaseHistoryPageDto.from(purchaseService.findForAdmin(principal.username, page, size, buyerUsername, packageName, from, to))
}
