package unq.pdes.backend.controller.dtos.requests

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import unq.pdes.backend.model.user.Role

@Schema(description = "Usuario creado por un admin. Si el rol es AGENCY, agencyId es obligatorio.")
data class CreateManagedUserRequestDto(
    @field:Schema(description = "Usuario único, de 3 a 60 letras, números, puntos, guiones o guiones bajos.", example = "vendedor1")
    @field:Pattern(
        regexp = "[A-Za-z0-9_.-]{3,60}",
        message = "El usuario debe tener entre 3 y 60 letras, números, puntos, guiones o guiones bajos.",
    )
    val username: String,
    @field:Schema(description = "Contraseña, de 6 a 72 caracteres.", example = "secreto123")
    @field:Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres.")
    @field:NotBlank
    val password: String,
    @field:Schema(description = "Nombre.", example = "María")
    @field:NotBlank
    @field:Size(max = 80)
    val firstName: String,
    @field:Schema(description = "Apellido.", example = "López")
    @field:NotBlank
    @field:Size(max = 80)
    val lastName: String,
    @field:Schema(description = "BUYER, AGENCY o ADMIN. AGENCY exige agencyId.")
    val role: Role,
    @field:Schema(description = "Agencia del vendedor. Obligatorio solo si el rol es AGENCY.")
    @field:Positive
    val agencyId: Long? = null,
)

@Schema(description = "Cambios de perfil y permisos. Un admin no puede apagarse ni bajarse el rol.")
data class UpdateManagedUserRequestDto(
    @field:Schema(description = "Nombre.", example = "María")
    @field:NotBlank
    @field:Size(max = 80)
    val firstName: String,
    @field:Schema(description = "Apellido.", example = "López")
    @field:NotBlank
    @field:Size(max = 80)
    val lastName: String,
    @field:Schema(description = "BUYER, AGENCY o ADMIN. AGENCY exige agencyId.")
    val role: Role,
    @field:Schema(description = "false apaga la cuenta. El próximo pedido con su token responde 401.", example = "true")
    @field:NotNull(message = "El estado de la cuenta es obligatorio.")
    val enabled: Boolean?,
    @field:Schema(description = "Agencia del vendedor. Obligatorio solo si el rol es AGENCY.")
    @field:Positive
    val agencyId: Long? = null,
)
