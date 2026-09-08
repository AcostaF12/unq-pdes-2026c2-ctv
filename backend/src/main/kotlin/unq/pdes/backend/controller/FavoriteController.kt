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

@Tag(name = "Favorite Services", description = "Favoritos del comprador.")
@RestController
@RequestMapping("/favorites")
class FavoriteController(
    private val favoriteService: FavoriteService,
) {

    @Operation(summary = "Mis favoritos", description = "Lista los paquetes favoritos del comprador.")
    @GetMapping
    fun mine(@AuthenticationPrincipal principal: UserDetails): ResponseEntity<List<FavoriteDto>> {
        return ResponseEntity.ok(favoriteService.findMine(principal.username).map { FavoriteDto.fromModel(it) })
    }

    @Operation(summary = "Agregar favorito", description = "Guarda un paquete como favorito.")
    @PostMapping
    fun add(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: FavoriteRequestDto,
    ): ResponseEntity<FavoriteDto> {
        val favorite = favoriteService.add(principal.username, request.packageId)
        return ResponseEntity.status(HttpStatus.CREATED).body(FavoriteDto.fromModel(favorite))
    }

    @Operation(summary = "Quitar favorito", description = "Elimina un paquete de favoritos.")
    @DeleteMapping("/{packageId}")
    fun remove(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable packageId: Long,
    ): ResponseEntity<Void> {
        favoriteService.remove(principal.username, packageId)
        return ResponseEntity.noContent().build()
    }
}
