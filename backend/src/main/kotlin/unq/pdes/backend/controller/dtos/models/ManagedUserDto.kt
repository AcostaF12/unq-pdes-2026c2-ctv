package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.User

data class ManagedUserDto(
    val id: Long,
    val username: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val enabled: Boolean,
    val agency: AgencyDto?,
) {
    companion object {
        fun fromModel(user: User): ManagedUserDto =
            ManagedUserDto(
                user.id!!,
                user.username,
                user.firstName,
                user.lastName,
                user.role.name,
                user.enabled,
                (user as? AgencyUser)?.agency?.let(AgencyDto::fromModel),
            )
    }
}
