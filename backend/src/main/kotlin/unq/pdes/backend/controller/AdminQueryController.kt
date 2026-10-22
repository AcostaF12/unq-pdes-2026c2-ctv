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

@Tag(name = "Administrative queries", description = "Solo ADMIN. Lectura paginada: page desde 0 y size entre 1 y 100.")
@RestController
@RequestMapping("/admin")
class AdminQueryController(
    private val service: AdminQueryService,
) {
    @Operation(summary = "Consultar favoritos de todos los compradores")
    @GetMapping("/favorites")
    fun favorites(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageDto<AdminFavoriteDto> = PageDto.from(service.favorites(page, size)) { AdminFavoriteDto.fromModel(it) }

    @Operation(summary = "Consultar puntajes y comentarios")
    @GetMapping("/reviews")
    fun reviews(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageDto<AdminReviewDto> = PageDto.from(service.reviews(page, size)) { AdminReviewDto.fromModel(it) }

    @Operation(summary = "Consultar compras", description = "Usa las copias históricas del comprador y el viaje y el precio abonado.")
    @GetMapping("/purchases")
    fun purchases(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageDto<PurchaseDto> = PageDto.from(service.purchases(page, size)) { PurchaseDto.fromModel(it) }
}
