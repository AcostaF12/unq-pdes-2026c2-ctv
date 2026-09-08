package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.UserDto
import unq.pdes.backend.controller.dtos.requests.LoginRequestDto
import unq.pdes.backend.controller.dtos.requests.RegisterRequestDto
import unq.pdes.backend.controller.dtos.responses.AuthResponseDto
import unq.pdes.backend.service.AuthService

@Tag(name = "Auth Services", description = "Registro y autenticación de usuarios (JWT).")
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    @Operation(summary = "Registrar comprador", description = "Crea un usuario COMPRADOR y devuelve su token JWT.")
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequestDto): ResponseEntity<AuthResponseDto> {
        val (token, user) = authService.register(
            request.username,
            request.password,
            request.firstName,
            request.lastName,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponseDto(token, UserDto.fromModel(user)))
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario y devuelve su token JWT.")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequestDto): ResponseEntity<AuthResponseDto> {
        val (token, user) = authService.login(request.username, request.password)
        return ResponseEntity.ok(AuthResponseDto(token, UserDto.fromModel(user)))
    }
}
