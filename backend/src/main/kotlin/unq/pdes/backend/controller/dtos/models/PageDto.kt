package unq.pdes.backend.controller.dtos.models

import org.springframework.data.domain.Page

data class PageDto<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T : Any, R> from(page: Page<T>, mapper: (T) -> R): PageDto<R> {
            return PageDto(
                content = page.content.map(mapper),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            )
        }
    }
}
