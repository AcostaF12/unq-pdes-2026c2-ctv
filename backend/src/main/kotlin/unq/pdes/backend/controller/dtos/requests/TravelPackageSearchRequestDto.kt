package unq.pdes.backend.controller.dtos.requests

import unq.pdes.backend.search.TravelPackageSearchCriteria
import java.math.BigDecimal

data class TravelPackageSearchRequestDto(
    val name: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
) {
    fun toCriteria(): TravelPackageSearchCriteria = TravelPackageSearchCriteria.from(name, origin, destination, minPrice, maxPrice)
}
