package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.AgencyDto
import unq.pdes.backend.controller.dtos.requests.CreateAgencyRequestDto
import unq.pdes.backend.controller.dtos.requests.UpdateAgencyRequestDto
import unq.pdes.backend.service.AgencyService

@Tag(name = "Agency Services", description = "Consulta de agencias.")
@RestController
@RequestMapping("/agencies")
class AgencyController(
    private val agencyService: AgencyService,
) {
    @Operation(summary = "Listar agencias", description = "Retorna todas las agencias registradas, ordenadas por nombre.")
    @GetMapping
    fun all(): ResponseEntity<List<AgencyDto>> = ResponseEntity.ok(agencyService.findAll().map { AgencyDto.fromModel(it) })

    @Operation(summary = "Crear agencia y su cuenta", description = "Solo ADMIN. Crea ambas en una transacción.")
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateAgencyRequestDto,
    ): ResponseEntity<AgencyDto> =
        ResponseEntity
            .status(
                HttpStatus.CREATED,
            ).body(
                AgencyDto.fromModel(
                    agencyService.create(request.name, request.username, request.password, request.firstName, request.lastName),
                ),
            )

    @Operation(summary = "Editar o desactivar agencia", description = "Solo ADMIN. Conserva paquetes e historial.")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAgencyRequestDto,
    ): ResponseEntity<AgencyDto> =
        ResponseEntity.ok(AgencyDto.fromModel(agencyService.update(id, request.name, requireNotNull(request.active))))
}
