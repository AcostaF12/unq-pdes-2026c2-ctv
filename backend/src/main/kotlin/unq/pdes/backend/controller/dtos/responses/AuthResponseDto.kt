package unq.pdes.backend.controller.dtos.responses

import unq.pdes.backend.controller.dtos.models.UserDto

data class AuthResponseDto(
    val token: String,
    val user: UserDto,
)
