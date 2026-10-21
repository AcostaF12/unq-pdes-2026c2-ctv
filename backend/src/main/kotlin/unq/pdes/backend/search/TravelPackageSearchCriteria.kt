package unq.pdes.backend.search

import java.math.BigDecimal
import java.util.Locale

data class TravelPackageSearchCriteria(
    val name: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val agencyId: Long? = null,
    val hotel: String? = null,
    val minRating: BigDecimal? = null,
) {
    init {
        require(minPrice == null || minPrice.signum() >= 0) { "The minimum price cannot be negative." }
        require(maxPrice == null || maxPrice.signum() >= 0) { "The maximum price cannot be negative." }
        require(minPrice == null || maxPrice == null || minPrice <= maxPrice) {
            "The minimum price cannot be greater than the maximum price."
        }
        require(minRating == null || minRating in MIN_RATING..MAX_RATING) {
            "The minimum rating must be between 0 and 10."
        }
    }

    fun isEmpty(): Boolean =
        name == null &&
            origin == null &&
            destination == null &&
            minPrice == null &&
            maxPrice == null &&
            agencyId == null &&
            hotel == null &&
            minRating == null

    companion object {
        private val MIN_RATING = BigDecimal.ZERO
        private val MAX_RATING = BigDecimal.TEN

        @Suppress("LongParameterList")
        fun from(
            name: String?,
            origin: String?,
            destination: String?,
            minPrice: BigDecimal?,
            maxPrice: BigDecimal?,
            agencyId: Long? = null,
            hotel: String? = null,
            minRating: BigDecimal? = null,
        ): TravelPackageSearchCriteria =
            TravelPackageSearchCriteria(
                name = name.trimmedOrNull(),
                origin = origin.trimmedOrNull()?.uppercase(Locale.ROOT),
                destination = destination.trimmedOrNull()?.uppercase(Locale.ROOT),
                minPrice = minPrice,
                maxPrice = maxPrice,
                agencyId = agencyId,
                hotel = hotel.trimmedOrNull(),
                minRating = minRating,
            )

        private fun String?.trimmedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
