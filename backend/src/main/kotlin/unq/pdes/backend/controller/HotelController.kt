package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.HotelDto
import unq.pdes.backend.controller.dtos.requests.HotelRequestDto
import unq.pdes.backend.service.HotelService

@Tag(name = "Hotel Services", description = "Endpoints para la administración (ABM) de hoteles.")
@RestController
@RequestMapping("/hotels")
class HotelController(
    private val hotelService: HotelService,
) {

    @Operation(summary = "Listar hoteles", description = "Retorna todos los hoteles registrados.")
    @GetMapping
    fun all(): ResponseEntity<List<HotelDto>> {
        return ResponseEntity.ok(hotelService.findAll().map { HotelDto.fromModel(it) })
    }

    @Operation(summary = "Obtener hotel", description = "Retorna el hotel identificado por ID.")
    @GetMapping("/{id}")
    fun byId(@PathVariable id: Long): ResponseEntity<HotelDto> {
        return ResponseEntity.ok(HotelDto.fromModel(hotelService.findById(id)))
    }

    @Operation(summary = "Crear hotel", description = "Registra un nuevo hotel.")
    @PostMapping
    fun create(@Valid @RequestBody request: HotelRequestDto): ResponseEntity<HotelDto> {
        val hotel = hotelService.create(request.name, request.cityCode, request.photoUrl)
        return ResponseEntity.status(HttpStatus.CREATED).body(HotelDto.fromModel(hotel))
    }

    @Operation(summary = "Actualizar hotel", description = "Modifica el hotel identificado por ID.")
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: HotelRequestDto): ResponseEntity<HotelDto> {
        val hotel = hotelService.update(id, request.name, request.cityCode, request.photoUrl)
        return ResponseEntity.ok(HotelDto.fromModel(hotel))
    }

    @Operation(summary = "Eliminar hotel", description = "Elimina el hotel identificado por ID.")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        hotelService.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
