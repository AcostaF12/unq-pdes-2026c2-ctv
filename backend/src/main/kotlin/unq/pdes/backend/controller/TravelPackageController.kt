package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
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
import unq.pdes.backend.controller.dtos.requests.PackageStatusRequestDto
import unq.pdes.backend.controller.dtos.requests.TravelPackageRequestDto
import unq.pdes.backend.controller.dtos.requests.TravelPackageSearchRequestDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.service.ReviewService
import unq.pdes.backend.service.TravelPackageService

@Tag(
    name = "Package Services",
    description =
        "El catálogo. Buscar y ver el detalle puede cualquier usuario autenticado. " +
            "Crear, editar, borrar y archivar es de la agencia dueña o de un admin.",
)
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
            "Arma una página del catálogo publicado. La página 0 es la primera.\n\n" +
                "Filtra por nombre, origen, destino, precio, agencia, hotel y puntaje. " +
                "El orden va en sort: name, price, destination.name o rating.\n\n" +
                "**Quién:** cualquier usuario autenticado\n" +
                "**Nota:** un admin también ve ofertas de agencias inactivas",
    )
    @GetMapping
    fun search(
        @AuthenticationPrincipal principal: UserDetails,
        @ModelAttribute request: TravelPackageSearchRequestDto,
        @PageableDefault(size = 20, sort = ["name"]) pageable: Pageable,
    ): ResponseEntity<PageDto<TravelPackageDto>> {
        val packages =
            travelPackageService.search(
                request.toCriteria(),
                pageable,
                includeInactiveAgencies = principal.authorities.any { it.authority == "ROLE_ADMIN" },
            )
        val summaries = reviewService.summarize(packages.content.mapNotNull { it.id })
        return ResponseEntity.ok(PageDto.from(packages) { TravelPackageDto.fromModel(it, summaries[it.id]) })
    }

    @Operation(
        summary = "Paquetes de la agencia",
        description =
            "Lista los paquetes de la agencia del token, también los archivados.\n\n" +
                "No pagina. Un admin mira el catálogo completo en GET /admin/packages.\n\n" +
                "**Quién:** agencia",
    )
    @GetMapping("/agency")
    fun mine(
        @AuthenticationPrincipal principal: UserDetails,
    ): ResponseEntity<List<TravelPackageDto>> {
        val packages = travelPackageService.findMine(principal.username)
        val summaries = reviewService.summarize(packages.mapNotNull { it.id })
        return ResponseEntity.ok(packages.map { TravelPackageDto.fromModel(it, summaries[it.id]) })
    }

    @Operation(
        summary = "Detalle de paquete",
        description =
            "Abre un paquete con su hotel, sus reseñas y, si vuelos responde, la ida y la vuelta.\n\n" +
                "Si vuelos no responde, el paquete igual vuelve y los vuelos vienen vacíos.\n\n" +
                "**Quién:** cualquier usuario autenticado\n" +
                "**404** no existe",
    )
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
            "Publica una oferta con nombre, hotel, ida, vuelta y precio.\n\n" +
                "La agencia no manda agencyId: se usa la suya. Un admin sí tiene que mandarlo. " +
                "La vuelta sale del destino de la ida y el hotel está en esa ciudad.\n\n" +
                "**Quién:** agencia o admin\n" +
                "**201** oferta creada\n" +
                "**400** nombre repetido en la agencia, o la ruta no cierra",
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
            "Cambia una oferta que ya existe. Valen las mismas reglas de hotel y vuelos que al crearla.\n\n" +
                "La agencia solo edita las suyas. Un admin edita cualquiera e indica agencyId. " +
                "Si ya tiene compras, no se le cambia la agencia.\n\n" +
                "**Quién:** agencia dueña o admin",
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
        description =
            "Borra el paquete solo si nadie lo compró, reseñó ni marcó como favorito.\n\n" +
                "Si ya se vendió, se archiva con PUT /packages/{id}/status.\n\n" +
                "**Quién:** agencia dueña o admin\n" +
                "**204** borrado\n" +
                "**400** todavía tiene historia",
    )
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        travelPackageService.deleteById(principal.username, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Publicar o archivar paquete",
        description =
            "Archiva o vuelve a publicar una oferta, sin borrar compras ni reseñas.\n\n" +
                "active=false la saca del catálogo. active=true la publica solo si los vuelos siguen vigentes y con lugar.\n\n" +
                "**Quién:** agencia dueña o admin",
    )
    @PutMapping("/{id}/status")
    fun status(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: PackageStatusRequestDto,
    ): TravelPackageDto = TravelPackageDto.fromModel(travelPackageService.setActive(principal.username, id, request.active!!))
}
