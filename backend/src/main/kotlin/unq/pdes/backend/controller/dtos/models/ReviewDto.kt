package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Review

data class ReviewDto(
    val id: Long?,
    val buyerUsername: String,
    val packageId: Long?,
    val score: Int,
    val comment: String?,
) {
    companion object {
        fun fromModel(review: Review): ReviewDto {
            return ReviewDto(
                id = review.id,
                buyerUsername = review.buyer.username,
                packageId = review.travelPackage.id,
                score = review.score,
                comment = review.comment,
            )
        }
    }
}
