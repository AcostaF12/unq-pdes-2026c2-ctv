package unq.pdes.backend.search

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

object TravelPackageSearchPageable {
    private const val MAX_PAGE_SIZE = 50
    private val SORTABLE_PROPERTIES = setOf("name", "price")
    private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("name"))

    fun sanitize(pageable: Pageable): Pageable {
        val size = pageable.pageSize.coerceIn(1, MAX_PAGE_SIZE)
        val sort = if (pageable.sort.isSorted) validateSort(pageable.sort) else DEFAULT_SORT
        return PageRequest.of(pageable.pageNumber, size, sort)
    }

    private fun validateSort(sort: Sort): Sort {
        sort.forEach { order ->
            require(order.property in SORTABLE_PROPERTIES) {
                "Cannot sort packages by '${order.property}'."
            }
        }
        return sort
    }
}
