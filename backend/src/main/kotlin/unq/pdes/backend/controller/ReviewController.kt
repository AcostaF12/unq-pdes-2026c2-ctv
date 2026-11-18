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
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.ReviewDto
import unq.pdes.backend.controller.dtos.requests.ReviewRequestDto
import unq.pdes.backend.service.ReviewService

@Tag(
    name = "Review Services",
    description =
        "Puntaje y comentario de un paquete. Leerlas puede cualquier usuario autenticado. " +
            "Publicar es solo un comprador que ya lo compró.",
)
@RestController
@RequestMapping("/reviews")
class ReviewController(
    private val reviewService: ReviewService,
) {
    @Operation(
        summary = "Reseñas de un paquete",
        description =
            "Lista las reseñas de un paquete, sin paginar.\n\n" +
                "El detalle del paquete también las trae.\n\n" +
                "**Quién:** cualquier usuario autenticado",
    )
    @GetMapping("/package/{packageId}")
    fun byPackage(
        @PathVariable packageId: Long,
    ): ResponseEntity<List<ReviewDto>> = ResponseEntity.ok(reviewService.findByPackageId(packageId).map { ReviewDto.fromModel(it) })

    @Operation(
        summary = "Crear reseña",
        description =
            "Publica un puntaje de 0 a 10 y un comentario opcional.\n\n" +
                "Solo si ese comprador ya compró el paquete, y una sola vez.\n\n" +
                "**Quién:** comprador\n" +
                "**201** reseña publicada\n" +
                "**400** no lo compró, o ya opinó",
    )
    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: ReviewRequestDto,
    ): ResponseEntity<ReviewDto> {
        val review = reviewService.create(principal.username, request.packageId, request.score, request.comment)
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewDto.fromModel(review))
    }
}
