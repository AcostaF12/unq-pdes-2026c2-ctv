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

@Tag(name = "Administración de usuarios", description = "Usuarios, vendedores y permisos. Solo ADMIN.")
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
class AdminUserController(
    private val administration: UserAdministrationService,
) {
    @Operation(summary = "Listar usuarios", description = "Listado paginado sin contraseñas. Tamaño máximo: 100.")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ManagedUserPageDto = ManagedUserPageDto.from(administration.list(page, size))

    @Operation(summary = "Crear usuario", description = "BUYER, ADMIN o vendedor AGENCY con agencyId obligatorio.")
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
        description = "Cambia perfil, rol, agencia y estado conservando el historial. No permite autobloqueo ni auto degradación.",
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
