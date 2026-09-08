package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.UserDto
import unq.pdes.backend.controller.dtos.requests.ChangePasswordRequestDto
import unq.pdes.backend.controller.dtos.requests.UpdateProfileRequestDto
import unq.pdes.backend.service.UserService

@Tag(name = "User Services", description = "Consulta y actualización del usuario autenticado.")
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {

    @Operation(summary = "Usuario actual", description = "Retorna los datos del usuario autenticado (según el token JWT).")
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: UserDetails): ResponseEntity<UserDto> {
        val user = userService.findByUsername(principal.username)
        return ResponseEntity.ok(UserDto.fromModel(user))
    }

    @Operation(summary = "Actualizar perfil", description = "Actualiza nombre y apellido del usuario autenticado.")
    @PatchMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: UpdateProfileRequestDto,
    ): ResponseEntity<UserDto> {
        val user = userService.updateProfile(principal.username, request.firstName, request.lastName)
        return ResponseEntity.ok(UserDto.fromModel(user))
    }

    @Operation(
        summary = "Cambiar contraseña",
        description = "Cambia la contraseña del usuario autenticado validando la actual.",
    )
    @PutMapping("/me/password")
    fun changePassword(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody request: ChangePasswordRequestDto,
    ): ResponseEntity<Void> {
        userService.changePassword(principal.username, request.currentPassword, request.newPassword)
        return ResponseEntity.noContent().build()
    }
}
