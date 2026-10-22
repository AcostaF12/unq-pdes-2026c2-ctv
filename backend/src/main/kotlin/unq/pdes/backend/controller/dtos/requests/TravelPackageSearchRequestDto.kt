package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import unq.pdes.backend.search.TravelPackageSearchCriteria
import java.math.BigDecimal

data class TravelPackageSearchRequestDto(
    val name: String? = null,
    @field:Schema(description = "Código IATA de la ciudad de origen, por ejemplo BUE.")
    val origin: String? = null,
    @field:Schema(description = "Código IATA de la ciudad de destino, por ejemplo PAR.")
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
