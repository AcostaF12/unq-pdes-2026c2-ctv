package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.ManagedUserDto
import unq.pdes.backend.controller.dtos.models.ManagedUserPageDto
import unq.pdes.backend.controller.dtos.requests.CreateManagedUserRequestDto
import unq.pdes.backend.controller.dtos.requests.UpdateManagedUserRequestDto
import unq.pdes.backend.service.UserAdministrationService

@Tag(
    name = "Administración de usuarios",
    description = "Alta y permisos de personas. Solo admin. Nunca devuelve contraseñas. Deshabilitar no borra el historial.",
)
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
class AdminUserController(
    private val administration: UserAdministrationService,
) {
    @Operation(
        summary = "Listar usuarios",
        description =
            "Página de usuarios, sin contraseñas. La página 0 es la primera.\n\n" +
                "size va de 1 a 100. El default es 20.\n\n" +
                "**Quién:** admin",
    )
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ManagedUserPageDto = ManagedUserPageDto.from(administration.list(page, size))

    @Operation(
        summary = "Crear usuario",
        description =
            "Crea un comprador, un admin o un vendedor.\n\n" +
                "Si el rol es AGENCY, agencyId es obligatorio y la agencia tiene que existir.\n\n" +
                "**Quién:** admin\n" +
                "**201** usuario creado\n" +
                "**400** el usuario ya existe",
    )
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateManagedUserRequestDto,
    ): ResponseEntity<ManagedUserDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            ManagedUserDto.fromModel(
                administration.create(
                    request.username,
                    request.password,
                    request.firstName,
                    request.lastName,
                    request.role,
                    request.agencyId,
                ),
            ),
        )

    @Operation(
        summary = "Actualizar usuario y permisos",
        description =
            "Cambia nombre, rol, agencia y si la cuenta sigue habilitada.\n\n" +
                "No borra compras ni reseñas. Un admin no puede apagarse ni bajarse el rol. " +
                "Si la cuenta queda apagada, el próximo pedido con su token responde 401.\n\n" +
                "**Quién:** admin",
    )
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateManagedUserRequestDto,
    ): ManagedUserDto =
        ManagedUserDto.fromModel(
            administration.update(
                principal.username,
                id,
                request.firstName,
                request.lastName,
                request.role,
                request.enabled!!,
                request.agencyId,
            ),
        )
}
