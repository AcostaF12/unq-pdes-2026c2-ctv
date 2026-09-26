package unq.pdes.backend.controller.dtos.models

import org.springframework.data.domain.Page
import unq.pdes.backend.model.Purchase

data class PurchaseHistoryPageDto(
    val content: List<PurchaseDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
) {
    companion object {
        fun from(page: Page<Purchase>) = PurchaseHistoryPageDto(
            content = page.content.map(PurchaseDto::fromModel),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            first = page.isFirst,
            last = page.isLast,
        )
    }
}
