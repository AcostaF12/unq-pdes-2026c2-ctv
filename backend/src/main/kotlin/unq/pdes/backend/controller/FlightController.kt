package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.FlightDto
import unq.pdes.backend.controller.dtos.requests.FlightRequestDto
import unq.pdes.backend.service.FlightService
import java.time.LocalDate

@Tag(name = "Flight Services", description = "Consulta y carga de vuelos (servicio externo de vuelos).")
@RestController
@RequestMapping("/flights")
class FlightController(
    private val flightService: FlightService,
) {
    @Operation(
        summary = "Buscar vuelos",
        description = "Lista los vuelos filtrando opcionalmente por origen, destino y fecha (yyyy-MM-dd).",
    )
    @GetMapping
    fun search(
        @Parameter(description = "Código de ciudad de origen") @RequestParam(required = false) origin: String?,
        @Parameter(description = "Código de ciudad de destino") @RequestParam(required = false) destination: String?,
        @Parameter(description = "Fecha del vuelo (yyyy-MM-dd)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
    ): ResponseEntity<List<FlightDto>> {
        val flights = flightService.search(origin, destination, date)
        return ResponseEntity.ok(flights.map { FlightDto.fromExternal(it) })
    }

    @Operation(summary = "Obtener vuelo", description = "Retorna el vuelo identificado por ID.")
    @GetMapping("/{id}")
    fun byId(
        @PathVariable id: Long,
    ): ResponseEntity<FlightDto> = ResponseEntity.ok(FlightDto.fromExternal(flightService.findById(id)))

    @Operation(summary = "Cargar vuelo", description = "Registra un nuevo vuelo en el servicio de vuelos. Solo ADMIN.")
    @PostMapping
    fun create(
        @Valid @RequestBody request: FlightRequestDto,
    ): ResponseEntity<FlightDto> {
        val flight =
            flightService.create(
                airline = request.airline,
                flightDate = request.flightDate,
                departureTime = request.departureTime,
                origin = request.origin,
                destination = request.destination,
                capacity = request.capacity,
                availability = request.availability,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(FlightDto.fromExternal(flight))
    }
}
