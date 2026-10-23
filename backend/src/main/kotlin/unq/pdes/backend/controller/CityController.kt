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

@Tag(
    name = "City Services",
    description =
        "Ciudades con código, para armar orígenes y destinos. " +
            "Cualquier usuario autenticado. No se crean por esta API.",
)
@RestController
@CrossOrigin
@RequestMapping("/cities")
class CityController(
    private val cityService: CityService,
) {
    @Operation(
        summary = "Listar ciudades",
        description =
            "Lista todas las ciudades, sin paginar.\n\n" +
                "El código se usa después como origen o destino de un hotel, un vuelo o un filtro.\n\n" +
                "**Quién:** cualquier usuario autenticado",
    )
    @GetMapping
    fun all(): ResponseEntity<List<CityDto>> = ResponseEntity.ok(cityService.findAll().map { CityDto.fromModel(it) })
}
