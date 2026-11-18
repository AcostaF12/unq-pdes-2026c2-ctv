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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.FavoriteDto
import unq.pdes.backend.controller.dtos.requests.FavoriteRequestDto
import unq.pdes.backend.service.FavoriteService
import unq.pdes.backend.service.ReviewService

@Tag(name = "Favorite Services", description = "Paquetes marcados por el comprador. No reservan lugar ni crean una compra.")
@RestController
@RequestMapping("/favorites")
class FavoriteController(
    private val favoriteService: FavoriteService,
    private val reviewService: ReviewService,
) {
    @Operation(
        summary = "Mis favoritos",
        description =
            "Lista los paquetes marcados por el usuario del token, con el resumen de reseñas.\n\n" +
                "No pagina y no reserva lugar.\n\n" +
                "**Quién:** el usuario autenticado. Agregar y quitar es solo comprador",
    )
    @GetMapping
    fun mine(
        @AuthenticationPrincipal principal: UserDetails,
    ): ResponseEntity<List<FavoriteDto>> {
        val favorites = favoriteService.findMine(principal.username)
        val summaries = reviewService.summarize(favorites.mapNotNull { it.travelPackage.id })
        return ResponseEntity.ok(favorites.map { FavoriteDto.fromModel(it, summaries[it.travelPackage.id]) })
    }

    @Operation(
        summary = "Agregar favorito",
        description =
            "Marca un paquete. No descuenta asientos.\n\n" +
                "**Quién:** comprador\n" +
                "**201** favorito guardado\n" +
                "**400** ya estaba marcado",
    )
    @PostMapping
    fun add(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: FavoriteRequestDto,
    ): ResponseEntity<FavoriteDto> {
        val favorite = favoriteService.add(principal.username, request.packageId)
        return ResponseEntity.status(HttpStatus.CREATED).body(FavoriteDto.fromModel(favorite))
    }

    @Operation(
        summary = "Quitar favorito",
        description =
            "Saca el paquete de favoritos. No borra la oferta ni las compras.\n\n" +
                "**Quién:** comprador\n" +
                "**204** favorito quitado\n" +
                "**404** no estaba marcado",
    )
    @DeleteMapping("/{packageId}")
    fun remove(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable packageId: Long,
    ): ResponseEntity<Void> {
        favoriteService.remove(principal.username, packageId)
        return ResponseEntity.noContent().build()
    }
}
