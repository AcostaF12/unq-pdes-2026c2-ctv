package unq.pdes.backend.controller.dtos.models

import org.springframework.data.domain.Page
import unq.pdes.backend.model.user.User

data class ManagedUserPageDto(
    val content: List<ManagedUserDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(result: Page<User>): ManagedUserPageDto =
            ManagedUserPageDto(
                result.content.map(ManagedUserDto::fromModel),
                result.number,
                result.size,
                result.totalElements,
                result.totalPages,
            )
    }
}
