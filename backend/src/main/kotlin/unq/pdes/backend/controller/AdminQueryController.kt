package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.AdminFavoriteDto
import unq.pdes.backend.controller.dtos.models.AdminReviewDto
import unq.pdes.backend.controller.dtos.models.PageDto
import unq.pdes.backend.controller.dtos.models.PurchaseDto
import unq.pdes.backend.service.AdminQueryService

@Tag(
    name = "Administrative queries",
    description = "Lectura de favoritos, reseñas y compras de todo el sistema. Solo admin. page empieza en 0 y size va de 1 a 100.",
)
@RestController
@RequestMapping("/admin")
class AdminQueryController(
    private val service: AdminQueryService,
) {
    @Operation(
        summary = "Consultar favoritos de todos los compradores",
        description =
            "Página de favoritos de todos los compradores: quién marcó qué paquete.\n\n" +
                "No es la lista personal de GET /favorites.\n\n" +
                "**Quién:** admin",
    )
    @GetMapping("/favorites")
    fun favorites(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageDto<AdminFavoriteDto> = PageDto.from(service.favorites(page, size)) { AdminFavoriteDto.fromModel(it) }

    @Operation(
        summary = "Consultar puntajes y comentarios",
        description =
            "Página de todas las reseñas: comprador, paquete, puntaje y comentario.\n\n" +
                "**Quién:** admin",
    )
    @GetMapping("/reviews")
    fun reviews(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageDto<AdminReviewDto> = PageDto.from(service.reviews(page, size)) { AdminReviewDto.fromModel(it) }

    @Operation(
        summary = "Consultar compras",
        description =
            "Página de todas las compras, con la copia de lo que se pagó.\n\n" +
                "Esa copia no cambia si después se edita el paquete.\n\n" +
                "**Quién:** admin",
    )
    @GetMapping("/purchases")
    fun purchases(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageDto<PurchaseDto> = PageDto.from(service.purchases(page, size)) { PurchaseDto.fromModel(it) }
}
