package unq.pdes.backend.controller.dtos.requests

import unq.pdes.backend.search.TravelPackageSearchCriteria
import java.math.BigDecimal

data class TravelPackageSearchRequestDto(
    val name: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val agencyId: Long? = null,
    val hotel: String? = null,
    val minRating: BigDecimal? = null,
) {
    fun toCriteria(): TravelPackageSearchCriteria =
        TravelPackageSearchCriteria.from(name, origin, destination, minPrice, maxPrice, agencyId, hotel, minRating)
}
