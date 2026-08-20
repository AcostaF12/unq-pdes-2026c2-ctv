package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.UserDto
import unq.pdes.backend.service.UserService

@Tag(name = "User Services", description = "Consulta de datos del usuario autenticado.")
@RestController
@CrossOrigin
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
}
