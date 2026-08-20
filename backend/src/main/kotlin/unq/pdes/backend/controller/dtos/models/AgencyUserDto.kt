package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.user.AgencyUser

class AgencyUserDto(
    id: Long?,
    username: String,
    role: String,
    firstName: String,
    lastName: String,
    val agency: AgencyDto,
) : UserDto(id, username, role, firstName, lastName) {
    companion object {
        fun fromModel(user: AgencyUser): AgencyUserDto {
            return AgencyUserDto(
                id = user.id,
                username = user.username,
                role = user.role.name,
                firstName = user.firstName,
                lastName = user.lastName,
                agency = AgencyDto.fromModel(user.agency),
            )
        }
    }
}
