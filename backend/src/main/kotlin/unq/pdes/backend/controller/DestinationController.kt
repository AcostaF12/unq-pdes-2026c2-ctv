package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.DestinationDto
import unq.pdes.backend.service.DestinationService

@Tag(name = "Destination Services", description = "Endpoints de consulta de destinos (IATA).")
@RestController
@CrossOrigin
@RequestMapping("/destinations")
class DestinationController(
    private val destinationService: DestinationService,
) {

    @Operation(summary = "Listar destinos", description = "Retorna todos los destinos disponibles.")
    @GetMapping
    fun all(): ResponseEntity<List<DestinationDto>> {
        return ResponseEntity.ok(destinationService.findAll().map { DestinationDto.fromModel(it) })
    }
}
