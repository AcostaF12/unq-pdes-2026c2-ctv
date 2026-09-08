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

@Tag(name = "Review Services", description = "Reseñas de paquetes.")
@RestController
@RequestMapping("/reviews")
class ReviewController(
    private val reviewService: ReviewService,
) {

    @Operation(summary = "Reseñas de un paquete", description = "Lista las reseñas asociadas a un paquete.")
    @GetMapping("/package/{packageId}")
    fun byPackage(@PathVariable packageId: Long): ResponseEntity<List<ReviewDto>> {
        return ResponseEntity.ok(reviewService.findByPackageId(packageId).map { ReviewDto.fromModel(it) })
    }

    @Operation(summary = "Crear reseña", description = "Publica una reseña de un paquete comprado.")
    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: ReviewRequestDto,
    ): ResponseEntity<ReviewDto> {
        val review = reviewService.create(principal.username, request.packageId, request.score, request.comment)
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewDto.fromModel(review))
    }
}
