package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.PageDto
import unq.pdes.backend.controller.dtos.models.TravelPackageDetailDto
import unq.pdes.backend.controller.dtos.models.TravelPackageDto
import unq.pdes.backend.controller.dtos.requests.TravelPackageRequestDto
import unq.pdes.backend.controller.dtos.requests.TravelPackageSearchRequestDto
import unq.pdes.backend.controller.dtos.responses.ErrorDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.service.ReviewService
import unq.pdes.backend.service.TravelPackageService

@Tag(name = "Package Services", description = "Búsqueda y administración de paquetes turísticos.")
@RestController
@RequestMapping("/packages")
class TravelPackageController(
    private val travelPackageService: TravelPackageService,
    private val reviewService: ReviewService,
    private val flightsClient: FlightsClient,
) {
    @Operation(
        summary = "Buscar paquetes",
        description =
            "Cualquier usuario autenticado. Filtra por nombre, origen y destino (código IATA), rango de precio " +
                "inclusivo, agencia ('agencyId'), hotel ('hotel') y puntaje promedio mínimo ('minRating', 0 a 10). " +
                "Paginación con 'page' y 'size'. Orden: 'sort=name,asc', 'price,desc', 'destination.name,asc' o " +
                "'rating,desc'. Otro campo de orden responde 400.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Página de paquetes."),
        ApiResponse(
            responseCode = "400",
            description = "El orden no es uno de los permitidos.",
            content = [Content(schema = Schema(implementation = ErrorDto::class))],
        ),
    )
    @GetMapping
    fun search(
        @ModelAttribute request: TravelPackageSearchRequestDto,
        @PageableDefault(size = 20, sort = ["name"]) pageable: Pageable,
    ): ResponseEntity<PageDto<TravelPackageDto>> {
        val packages = travelPackageService.search(request.toCriteria(), pageable)
        val summaries = reviewService.summarize(packages.content.mapNotNull { it.id })
        return ResponseEntity.ok(PageDto.from(packages) { TravelPackageDto.fromModel(it, summaries[it.id]) })
    }

    @Operation(summary = "Paquetes de la agencia", description = "Lista los paquetes de la agencia autenticada.")
    @GetMapping("/agency")
    fun mine(
        @AuthenticationPrincipal principal: UserDetails,
    ): ResponseEntity<List<TravelPackageDto>> {
        val packages = travelPackageService.findMine(principal.username)
        val summaries = reviewService.summarize(packages.mapNotNull { it.id })
        return ResponseEntity.ok(packages.map { TravelPackageDto.fromModel(it, summaries[it.id]) })
    }

    @Operation(summary = "Detalle de paquete", description = "Incluye vuelos y reseñas cuando están disponibles.")
    @GetMapping("/{id}")
    fun byId(
        @PathVariable id: Long,
    ): ResponseEntity<TravelPackageDetailDto> {
        val travelPackage = travelPackageService.findById(id)
        val reviews = reviewService.findByPackageId(id)
        val outbound = runCatching { flightsClient.findById(travelPackage.outboundFlightId) }.getOrNull()
        val returnFlight = runCatching { flightsClient.findById(travelPackage.returnFlightId) }.getOrNull()
        return ResponseEntity.ok(TravelPackageDetailDto.from(travelPackage, outbound, returnFlight, reviews))
    }

    @Operation(
        summary = "Crear paquete",
        description =
            "Registra un paquete para la agencia autenticada. Un ADMIN debe indicar 'agencyId' y crea el paquete " +
                "para esa agencia; las agencias ignoran 'agencyId'.",
    )
    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: TravelPackageRequestDto,
    ): ResponseEntity<TravelPackageDto> {
        val travelPackage =
            travelPackageService.create(
                username = principal.username,
                name = request.name,
                hotelId = request.hotelId,
                outboundFlightId = request.outboundFlightId,
                returnFlightId = request.returnFlightId,
                price = request.price,
                agencyId = request.agencyId,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(TravelPackageDto.fromModel(travelPackage))
    }

    @Operation(
        summary = "Actualizar paquete",
        description =
            "Modifica un paquete de la agencia autenticada. Un ADMIN puede modificar cualquier paquete e indica " +
                "la agencia con 'agencyId' (obligatorio).",
    )
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: TravelPackageRequestDto,
    ): ResponseEntity<TravelPackageDto> {
        val travelPackage =
            travelPackageService.update(
                username = principal.username,
                id = id,
                name = request.name,
                hotelId = request.hotelId,
                outboundFlightId = request.outboundFlightId,
                returnFlightId = request.returnFlightId,
                price = request.price,
                agencyId = request.agencyId,
            )
        return ResponseEntity.ok(TravelPackageDto.fromModel(travelPackage))
    }

    @Operation(
        summary = "Eliminar paquete",
        description = "Elimina un paquete de la agencia autenticada. Un ADMIN puede eliminar cualquier paquete.",
    )
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        travelPackageService.deleteById(principal.username, id)
        return ResponseEntity.noContent().build()
    }
}
