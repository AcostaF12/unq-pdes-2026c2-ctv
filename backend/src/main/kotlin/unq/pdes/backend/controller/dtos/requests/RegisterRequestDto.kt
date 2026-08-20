package unq.pdes.backend.controller.dtos.requests

data class RegisterRequestDto(
    val username: String,
    val password: String,
    val firstName: String,
    val lastName: String,
)
