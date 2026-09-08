package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Favorite

data class FavoriteDto(
    val id: Long?,
    val packageId: Long?,
    val travelPackage: TravelPackageDto,
) {
    companion object {
        fun fromModel(favorite: Favorite): FavoriteDto {
            return FavoriteDto(
                id = favorite.id,
                packageId = favorite.travelPackage.id,
                travelPackage = TravelPackageDto.fromModel(favorite.travelPackage),
            )
        }
    }
}
