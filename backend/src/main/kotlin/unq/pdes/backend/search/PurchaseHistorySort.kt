package unq.pdes.backend.search

import org.springframework.data.domain.Sort
import java.util.Locale

object PurchaseHistorySort {
    private const val DEFAULT_OPTION = "date,desc"
    private val OPTIONS =
        mapOf(
            DEFAULT_OPTION to Sort.Order.desc("purchasedAt"),
            "date,asc" to Sort.Order.asc("purchasedAt"),
            "price,asc" to Sort.Order.asc("purchasePrice"),
            "price,desc" to Sort.Order.desc("purchasePrice"),
        )

    /** Parses 'date,desc' (default), 'date,asc', 'price,asc' or 'price,desc'; the id breaks ties newest first. */
    fun parse(value: String?): Sort {
        val key = value?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() } ?: DEFAULT_OPTION
        val order = requireNotNull(OPTIONS[key]) { "Cannot sort purchases by '$value'." }
        return Sort.by(order, Sort.Order.desc("id"))
    }
}
