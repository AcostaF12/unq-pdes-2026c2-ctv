package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Favorite

data class AdminFavoriteDto(
    val id: Long?,
    val buyerUsername: String,
    val packageId: Long?,
    val packageName: String,
    val agencyName: String,
) {
    companion object {
        fun fromModel(favorite: Favorite): AdminFavoriteDto =
            AdminFavoriteDto(
                favorite.id,
                favorite.buyer.username,
                favorite.travelPackage.id,
                favorite.travelPackage.name,
                favorite.travelPackage.agency.name,
            )
    }
}
