package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
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

@Tag(
    name = "Auth Services",
    description = "Entrada pública. No pide token. Crea un comprador o abre una sesión y devuelve el token para Authorize.",
)
@SecurityRequirements
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @Operation(
        summary = "Registrar comprador",
        description =
            "Crea una cuenta de comprador y devuelve el token listo para Authorize.\n\n" +
                "No crea agencias ni administradores.\n\n" +
                "**Quién:** cualquiera, sin token\n" +
                "**201** cuenta creada\n" +
                "**400** ese usuario ya existe",
    )
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequestDto,
    ): ResponseEntity<AuthResponseDto> {
        val (token, user) =
            authService.register(
                request.username,
                request.password,
                request.firstName,
                request.lastName,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponseDto(token, UserDto.fromModel(user)))
    }

    @Operation(
        summary = "Iniciar sesión",
        description =
            "Comprueba usuario y contraseña y devuelve el token y el rol.\n\n" +
                "Ese token se pega en Authorize, sin la palabra Bearer.\n\n" +
                "**Quién:** cualquiera, sin token\n" +
                "**200** sesión abierta\n" +
                "**401** credenciales incorrectas o cuenta apagada",
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequestDto,
    ): ResponseEntity<AuthResponseDto> {
        val (token, user) = authService.login(request.username, request.password)
        return ResponseEntity.ok(AuthResponseDto(token, UserDto.fromModel(user)))
    }
}
