package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.PurchaseDetailDto
import unq.pdes.backend.controller.dtos.models.PurchaseDto
import unq.pdes.backend.controller.dtos.models.PurchaseHistoryPageDto
import unq.pdes.backend.controller.dtos.requests.PurchaseRequestDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.service.PurchaseService
import unq.pdes.backend.service.ReviewService
import java.math.BigDecimal
import java.time.LocalDate

@Tag(name = "Purchase Services", description = "Compra de paquetes e historial.")
@RestController
@RequestMapping("/purchases")
class PurchaseController(
    private val purchaseService: PurchaseService,
    private val reviewService: ReviewService,
    private val flightsClient: FlightsClient,
) {
    @Operation(
        summary = "Mis compras",
        description =
            "Lista las compras del usuario autenticado. Filtra por nombre del paquete, destino (código), " +
                "rango de fechas y rango de precio abonado. Orden: 'sort=date,desc' (por defecto), " +
                "'date,asc', 'price,asc' o 'price,desc'.",
    )
    @GetMapping("/me")
    @Suppress("LongParameterList")
    fun mine(
        @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) packageName: String?,
        @RequestParam(required = false) destination: String?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<PurchaseHistoryPageDto> =
        ResponseEntity.ok(
            PurchaseHistoryPageDto.from(
                purchaseService.findMine(
                    username = principal.username,
                    page = page,
                    size = size,
                    packageName = packageName,
                    destination = destination,
                    from = from,
                    to = to,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    sort = sort,
                ),
            ),
        )

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
    ): ResponseEntity<PurchaseHistoryPageDto> =
        ResponseEntity.ok(
            PurchaseHistoryPageDto.from(
                purchaseService.findForAgency(principal.username, page, size, buyerUsername, packageName, from, to),
            ),
        )

    @Operation(summary = "Verificar compra", description = "Indica si el usuario autenticado ya compró un paquete.")
    @GetMapping("/me/packages/{packageId}")
    fun hasPurchased(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable packageId: Long,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(purchaseService.hasPurchased(principal.username, packageId))

    @Operation(
        summary = "Detalle de compra",
        description =
            "Incluye los vuelos de ida y vuelta cuando están disponibles y, para el comprador, su reseña. " +
                "Solo el comprador o la agencia vendedora pueden consultarla.",
    )
    @GetMapping("/{id}")
    fun byId(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
    ): ResponseEntity<PurchaseDetailDto> {
        val purchase = purchaseService.findForViewer(principal.username, id)
        val outbound = runCatching { flightsClient.findById(purchase.travelSnapshot.outboundFlightId) }.getOrNull()
        val returnFlight = runCatching { flightsClient.findById(purchase.travelSnapshot.returnFlightId) }.getOrNull()
        val review =
            if (purchase.buyerSnapshot.username == principal.username) {
                reviewService.findByBuyerAndPackage(purchase.buyer.id!!, purchase.travelPackage.id!!)
            } else {
                null
            }
        return ResponseEntity.ok(PurchaseDetailDto.from(purchase, outbound, returnFlight, review))
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
