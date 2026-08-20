package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.User

open class UserDto(
    val id: Long?,
    val username: String,
    val role: String,
    val firstName: String,
    val lastName: String,
) {
    companion object {
        fun fromModel(user: User): UserDto {
            return when (user) {
                is AgencyUser -> AgencyUserDto.fromModel(user)
                else -> UserDto(
                    id = user.id,
                    username = user.username,
                    role = user.role.name,
                    firstName = user.firstName,
                    lastName = user.lastName,
                )
            }
        }
    }
}
