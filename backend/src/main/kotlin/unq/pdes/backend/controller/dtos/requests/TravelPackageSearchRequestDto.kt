package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import unq.pdes.backend.search.TravelPackageSearchCriteria
import java.math.BigDecimal

@Schema(description = "Filtros del catálogo. Todos son opcionales. La página y el orden van en page, size y sort.")
data class TravelPackageSearchRequestDto(
    @field:Schema(description = "Texto del nombre del paquete.")
    val name: String? = null,
    @field:Schema(description = "Código de la ciudad de origen.", example = "BUE")
    val origin: String? = null,
    @field:Schema(description = "Código de la ciudad de destino.", example = "PAR")
    val destination: String? = null,
    @field:Schema(description = "Precio mínimo, inclusive.")
    val minPrice: BigDecimal? = null,
    @field:Schema(description = "Precio máximo, inclusive.")
    val maxPrice: BigDecimal? = null,
    @field:Schema(description = "Id de la agencia.")
    val agencyId: Long? = null,
    @field:Schema(description = "Texto del nombre del hotel.")
    val hotel: String? = null,
    @field:Schema(description = "Puntaje promedio mínimo, de 0 a 10.")
    val minRating: BigDecimal? = null,
) {
    fun toCriteria(): TravelPackageSearchCriteria =
        TravelPackageSearchCriteria.from(name, origin, destination, minPrice, maxPrice, agencyId, hotel, minRating)
}
