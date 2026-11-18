package unq.pdes.backend.controller.dtos.requests

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import unq.pdes.backend.model.user.Role

data class CreateManagedUserRequestDto(
    @field:Pattern(
        regexp = "[A-Za-z0-9_.-]{3,60}",
        message = "El usuario debe tener entre 3 y 60 letras, números, puntos, guiones o guiones bajos.",
    )
    val username: String,
    @field:Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres.")
    @field:NotBlank
    val password: String,
    @field:NotBlank
    @field:Size(max = 80)
    val firstName: String,
    @field:NotBlank
    @field:Size(max = 80)
    val lastName: String,
    val role: Role,
    @field:Positive
    val agencyId: Long? = null,
)

data class UpdateManagedUserRequestDto(
    @field:NotBlank
    @field:Size(max = 80)
    val firstName: String,
    @field:NotBlank
    @field:Size(max = 80)
    val lastName: String,
    val role: Role,
    @field:NotNull(message = "El estado de la cuenta es obligatorio.")
    val enabled: Boolean?,
    @field:Positive
    val agencyId: Long? = null,
)
