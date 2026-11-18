package unq.pdes.backend.search

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

data class PurchaseHistorySearchCriteria(
    val packageName: String? = null,
    val destination: String? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
) {
    init {
        require(from == null || to == null || !from.isAfter(to)) { "The start date must not be after the end date." }
        require(minPrice == null || minPrice.signum() >= 0) { "The minimum price cannot be negative." }
        require(maxPrice == null || maxPrice.signum() >= 0) { "The maximum price cannot be negative." }
        require(minPrice == null || maxPrice == null || minPrice <= maxPrice) {
            "The minimum price cannot be greater than the maximum price."
        }
    }

    companion object {
        @Suppress("LongParameterList")
        fun from(
            packageName: String?,
            destination: String?,
            from: LocalDate?,
            to: LocalDate?,
            minPrice: BigDecimal?,
            maxPrice: BigDecimal?,
        ): PurchaseHistorySearchCriteria =
            PurchaseHistorySearchCriteria(
                packageName = packageName.trimmedOrNull(),
                destination = destination.trimmedOrNull()?.uppercase(Locale.ROOT),
                from = from,
                to = to,
                minPrice = minPrice,
                maxPrice = maxPrice,
            )

        private fun String?.trimmedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
