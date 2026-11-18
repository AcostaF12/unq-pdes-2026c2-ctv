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

@Tag(
    name = "Purchase Services",
    description =
        "La compra y el historial. Comprar y ver lo propio es del comprador. " +
            "Ver las ventas de su agencia es del rol agencia. El precio guardado no cambia si el paquete se edita después.",
)
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
            "Historial del comprador, de a una página. La página 0 es la primera.\n\n" +
                "El precio es el que se pagó, no el de hoy. " +
                "Se puede filtrar por paquete, destino, fechas y precio, " +
                "y ordenar con sort=date,desc, date,asc, price,asc o price,desc.\n\n" +
                "**Quién:** comprador",
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

    @Operation(
        summary = "Compras de la agencia",
        description =
            "Ventas de la agencia del token, de a una página.\n\n" +
                "Filtra por comprador, nombre del paquete y fechas. " +
                "El historial de todas las agencias está en GET /admin/sales.\n\n" +
                "**Quién:** agencia",
    )
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

    @Operation(
        summary = "Verificar compra",
        description =
            "Dice si este comprador ya compró el paquete.\n\n" +
                "La pantalla lo usa para mostrar \"Ya comprado\".\n\n" +
                "**Quién:** comprador\n" +
                "**200** true o false",
    )
    @GetMapping("/me/packages/{packageId}")
    fun hasPurchased(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable packageId: Long,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(purchaseService.hasPurchased(principal.username, packageId))

    @Operation(
        summary = "Detalle de compra",
        description =
            "Abre una compra con la copia de lo que se pagó y, si vuelos responde, la ida y la vuelta.\n\n" +
                "Si mira el comprador, también trae su reseña.\n\n" +
                "**Quién:** el comprador, la agencia que vendió o un admin\n" +
                "**403** otro rol\n" +
                "**404** no existe",
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

    @Operation(
        summary = "Comprar paquete",
        description =
            "Única ruta pública que vende asientos.\n\n" +
                "Vende la ida y después la vuelta. Si la vuelta falla, cancela la ida y no guarda la compra. " +
                "El ticket conserva el precio de ese momento.\n\n" +
                "**Quién:** comprador\n" +
                "**201** compra creada\n" +
                "**400** archivado, agencia inactiva o sin lugar\n" +
                "**404** el paquete no existe\n" +
                "**503** vuelos no responde",
    )
    @PostMapping
    fun purchase(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: PurchaseRequestDto,
    ): ResponseEntity<PurchaseDto> {
        val purchase = purchaseService.purchase(principal.username, request.packageId)
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseDto.fromModel(purchase))
    }
}
