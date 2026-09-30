package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.LocalDate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.PurchaseDto
import unq.pdes.backend.controller.dtos.models.PurchaseHistoryPageDto
import unq.pdes.backend.controller.dtos.requests.PurchaseRequestDto
import unq.pdes.backend.service.PurchaseService

@Tag(name = "Purchase Services", description = "Compra de paquetes e historial.")
@RestController
@RequestMapping("/purchases")
class PurchaseController(
    private val purchaseService: PurchaseService,
) {

    @Operation(summary = "Mis compras", description = "Lista las compras del usuario autenticado.")
    @GetMapping("/me")
    fun mine(
        @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PurchaseHistoryPageDto> {
        return ResponseEntity.ok(PurchaseHistoryPageDto.from(purchaseService.findMine(principal.username, page, size)))
    }

    @Operation(summary = "Compras de la agencia", description = "Lista las ventas de la agencia autenticada.")
    @GetMapping("/agency")
    fun agency(
        @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) buyerUsername: String?,
        @RequestParam(required = false) packageName: String?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): ResponseEntity<PurchaseHistoryPageDto> {
        return ResponseEntity.ok(
            PurchaseHistoryPageDto.from(
                purchaseService.findForAgency(principal.username, page, size, buyerUsername, packageName, from, to),
            ),
        )
    }

    @Operation(summary = "Verificar compra", description = "Indica si el usuario autenticado ya compró un paquete.")
    @GetMapping("/me/packages/{packageId}")
    fun hasPurchased(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable packageId: Long,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(purchaseService.hasPurchased(principal.username, packageId))

    @Operation(summary = "Comprar paquete", description = "Registra la compra y reserva asientos de ida y vuelta.")
    @PostMapping
    fun purchase(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: PurchaseRequestDto,
    ): ResponseEntity<PurchaseDto> {
        val purchase = purchaseService.purchase(principal.username, request.packageId)
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseDto.fromModel(purchase))
    }
}
