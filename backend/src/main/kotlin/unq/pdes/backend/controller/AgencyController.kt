package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.AgencyDto
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
}
