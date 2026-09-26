package unq.pdes.backend.search

import java.math.BigDecimal
import java.util.Locale

data class TravelPackageSearchCriteria(
    val name: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
) {
    init {
        require(minPrice == null || minPrice.signum() >= 0) { "The minimum price cannot be negative." }
        require(maxPrice == null || maxPrice.signum() >= 0) { "The maximum price cannot be negative." }
        require(minPrice == null || maxPrice == null || minPrice <= maxPrice) {
            "The minimum price cannot be greater than the maximum price."
        }
    }

    fun isEmpty(): Boolean = name == null && origin == null && destination == null && minPrice == null && maxPrice == null

    companion object {
        fun from(
            name: String?,
            origin: String?,
            destination: String?,
            minPrice: BigDecimal?,
            maxPrice: BigDecimal?,
        ): TravelPackageSearchCriteria {
            return TravelPackageSearchCriteria(
                name = name.trimmedOrNull(),
                origin = origin.trimmedOrNull()?.uppercase(Locale.ROOT),
                destination = destination.trimmedOrNull()?.uppercase(Locale.ROOT),
                minPrice = minPrice,
                maxPrice = maxPrice,
            )
        }

        private fun String?.trimmedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
