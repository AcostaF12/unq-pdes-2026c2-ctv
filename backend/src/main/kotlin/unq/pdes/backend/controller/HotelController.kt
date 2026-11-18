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

@Tag(
    name = "Hotel Services",
    description = "Hoteles de los paquetes. Leerlos puede cualquier usuario autenticado. Crear, editar y borrar es solo admin.",
)
@RestController
@RequestMapping("/hotels")
class HotelController(
    private val hotelService: HotelService,
) {
    @Operation(
        summary = "Listar hoteles",
        description =
            "Lista todos los hoteles, con ciudad y foto.\n\n" +
                "No pagina y no filtra por agencia.\n\n" +
                "**Quién:** cualquier usuario autenticado",
    )
    @GetMapping
    fun all(): ResponseEntity<List<HotelDto>> = ResponseEntity.ok(hotelService.findAll().map { HotelDto.fromModel(it) })

    @Operation(
        summary = "Obtener hotel",
        description =
            "Abre un hotel por id.\n\n" +
                "**Quién:** cualquier usuario autenticado\n" +
                "**404** no existe",
    )
    @GetMapping("/{id}")
    fun byId(
        @PathVariable id: Long,
    ): ResponseEntity<HotelDto> = ResponseEntity.ok(HotelDto.fromModel(hotelService.findById(id)))

    @Operation(
        summary = "Crear hotel",
        description =
            "Da de alta un hotel en una ciudad que ya existe.\n\n" +
                "El código sale de GET /cities.\n\n" +
                "**Quién:** admin\n" +
                "**201** hotel creado",
    )
    @PostMapping
    fun create(
        @Valid @RequestBody request: HotelRequestDto,
    ): ResponseEntity<HotelDto> {
        val hotel = hotelService.create(request.name, request.cityCode, request.photoUrl)
        return ResponseEntity.status(HttpStatus.CREATED).body(HotelDto.fromModel(hotel))
    }

    @Operation(
        summary = "Actualizar hotel",
        description =
            "Cambia el nombre, la ciudad o la foto.\n\n" +
                "No modifica las compras que ya se hicieron.\n\n" +
                "**Quién:** admin",
    )
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: HotelRequestDto,
    ): ResponseEntity<HotelDto> {
        val hotel = hotelService.update(id, request.name, request.cityCode, request.photoUrl)
        return ResponseEntity.ok(HotelDto.fromModel(hotel))
    }

    @Operation(
        summary = "Eliminar hotel",
        description =
            "Borra el hotel si ningún paquete lo usa.\n\n" +
                "Si hay una oferta apuntándolo, se conserva para no romper el historial.\n\n" +
                "**Quién:** admin\n" +
                "**204** borrado\n" +
                "**409** un paquete todavía lo usa",
    )
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        hotelService.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
