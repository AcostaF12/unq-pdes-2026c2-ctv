package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.TravelPackageDetailDto
import unq.pdes.backend.controller.dtos.models.TravelPackageDto
import unq.pdes.backend.controller.dtos.requests.TravelPackageRequestDto
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

    @Operation(summary = "Buscar paquetes", description = "Filtra por nombre, origen y destino.")
    @GetMapping
    fun search(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) origin: String?,
        @RequestParam(required = false) destination: String?,
    ): ResponseEntity<List<TravelPackageDto>> {
        val packages = travelPackageService.search(name, origin, destination)
        return ResponseEntity.ok(packages.map { TravelPackageDto.fromModel(it) })
    }

    @Operation(summary = "Paquetes de la agencia", description = "Lista los paquetes de la agencia autenticada.")
    @GetMapping("/agency")
    fun mine(@AuthenticationPrincipal principal: UserDetails): ResponseEntity<List<TravelPackageDto>> {
        val packages = travelPackageService.findMine(principal.username)
        return ResponseEntity.ok(packages.map { TravelPackageDto.fromModel(it) })
    }

    @Operation(summary = "Detalle de paquete", description = "Incluye vuelos y reseñas cuando están disponibles.")
    @GetMapping("/{id}")
    fun byId(@PathVariable id: Long): ResponseEntity<TravelPackageDetailDto> {
        val travelPackage = travelPackageService.findById(id)
        val reviews = reviewService.findByPackageId(id)
        val outbound = runCatching { flightsClient.findById(travelPackage.outboundFlightId) }.getOrNull()
        val returnFlight = runCatching { flightsClient.findById(travelPackage.returnFlightId) }.getOrNull()
        return ResponseEntity.ok(TravelPackageDetailDto.from(travelPackage, outbound, returnFlight, reviews))
    }

    @Operation(summary = "Crear paquete", description = "Registra un paquete para la agencia autenticada.")
    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: TravelPackageRequestDto,
    ): ResponseEntity<TravelPackageDto> {
        val travelPackage = travelPackageService.create(
            username = principal.username,
            name = request.name,
            hotelId = request.hotelId,
            outboundFlightId = request.outboundFlightId,
            returnFlightId = request.returnFlightId,
            price = request.price,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(TravelPackageDto.fromModel(travelPackage))
    }

    @Operation(summary = "Actualizar paquete", description = "Modifica un paquete de la agencia autenticada.")
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: TravelPackageRequestDto,
    ): ResponseEntity<TravelPackageDto> {
        val travelPackage = travelPackageService.update(
            username = principal.username,
            id = id,
            name = request.name,
            hotelId = request.hotelId,
            outboundFlightId = request.outboundFlightId,
            returnFlightId = request.returnFlightId,
            price = request.price,
        )
        return ResponseEntity.ok(TravelPackageDto.fromModel(travelPackage))
    }

    @Operation(summary = "Eliminar paquete", description = "Elimina un paquete de la agencia autenticada.")
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        travelPackageService.deleteById(principal.username, id)
        return ResponseEntity.noContent().build()
    }
}
