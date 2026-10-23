package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.PageDto
import unq.pdes.backend.controller.dtos.models.TravelPackageDto
import unq.pdes.backend.controller.dtos.requests.TravelPackageSearchRequestDto
import unq.pdes.backend.service.ReviewService
import unq.pdes.backend.service.TravelPackageService

@Tag(
    name = "Administración de paquetes",
    description = "Catálogo completo para el admin, incluidos los archivados. La agencia usa GET /packages/agency.",
)
@RestController
@PreAuthorize("hasRole('ADMIN')")
class AdminPackageController(
    private val packages: TravelPackageService,
    private val reviews: ReviewService,
) {
    @Operation(
        summary = "Catálogo administrativo",
        description =
            "La misma búsqueda del catálogo, incluyendo agencias inactivas.\n\n" +
                "Sin active entran solo los publicados. active=false muestra los archivados.\n\n" +
                "**Quién:** admin",
    )
    @GetMapping("/admin/packages")
    fun list(
        @ModelAttribute request: TravelPackageSearchRequestDto,
        @PageableDefault(size = 20, sort = ["name"]) pageable: Pageable,
        @RequestParam(required = false) active: Boolean?,
    ): PageDto<TravelPackageDto> {
        val result = packages.search(request.toCriteria(), pageable, active, includeInactiveAgencies = true)
        val summaries = reviews.summarize(result.content.mapNotNull { it.id })
        return PageDto.from(result) { TravelPackageDto.fromModel(it, summaries[it.id]) }
    }
}
