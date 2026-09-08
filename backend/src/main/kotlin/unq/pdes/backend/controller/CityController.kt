package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.CityDto
import unq.pdes.backend.service.CityService

@Tag(name = "City Services", description = "Endpoints de consulta de ciudades (IATA).")
@RestController
@CrossOrigin
@RequestMapping("/cities")
class CityController(
    private val cityService: CityService,
) {

    @Operation(summary = "Listar ciudades", description = "Retorna todas las ciudades disponibles.")
    @GetMapping
    fun all(): ResponseEntity<List<CityDto>> {
        return ResponseEntity.ok(cityService.findAll().map { CityDto.fromModel(it) })
    }
}
