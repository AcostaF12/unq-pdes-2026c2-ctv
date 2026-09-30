package unq.pdes.backend.controller.dtos.requests

import java.math.BigDecimal
import unq.pdes.backend.search.TravelPackageSearchCriteria

data class TravelPackageSearchRequestDto(
    val name: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
) {
    fun toCriteria(): TravelPackageSearchCriteria {
        return TravelPackageSearchCriteria.from(name, origin, destination, minPrice, maxPrice)
    }
}
