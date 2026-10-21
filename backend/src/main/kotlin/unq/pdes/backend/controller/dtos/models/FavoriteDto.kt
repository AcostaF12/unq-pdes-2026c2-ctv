package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Favorite
import unq.pdes.backend.model.ReviewSummary

data class FavoriteDto(
    val id: Long?,
    val packageId: Long?,
    val travelPackage: TravelPackageDto,
) {
    companion object {
        fun fromModel(
            favorite: Favorite,
            summary: ReviewSummary? = null,
        ): FavoriteDto =
            FavoriteDto(
                id = favorite.id,
                packageId = favorite.travelPackage.id,
                travelPackage = TravelPackageDto.fromModel(favorite.travelPackage, summary),
            )
    }
}
