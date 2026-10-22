package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Review

data class AdminReviewDto(
    val id: Long?,
    val buyerUsername: String,
    val packageId: Long?,
    val packageName: String,
    val score: Int,
    val comment: String?,
) {
    companion object {
        fun fromModel(review: Review): AdminReviewDto =
            AdminReviewDto(
                review.id,
                review.buyer.username,
                review.travelPackage.id,
                review.travelPackage.name,
                review.score,
                review.comment,
            )
    }
}
