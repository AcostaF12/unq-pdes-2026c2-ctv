package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.PurchaseDto
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
    fun mine(@AuthenticationPrincipal principal: UserDetails): ResponseEntity<List<PurchaseDto>> {
        return ResponseEntity.ok(purchaseService.findMine(principal.username).map { PurchaseDto.fromModel(it) })
    }

    @Operation(summary = "Compras de la agencia", description = "Lista las ventas de la agencia autenticada.")
    @GetMapping("/agency")
    fun agency(@AuthenticationPrincipal principal: UserDetails): ResponseEntity<List<PurchaseDto>> {
        return ResponseEntity.ok(purchaseService.findForAgency(principal.username).map { PurchaseDto.fromModel(it) })
    }

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
