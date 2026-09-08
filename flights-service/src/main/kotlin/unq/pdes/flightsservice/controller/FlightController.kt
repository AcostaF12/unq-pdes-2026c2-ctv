package unq.pdes.flightsservice.controller

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import unq.pdes.flightsservice.controller.dtos.models.FlightDto
import unq.pdes.flightsservice.controller.dtos.models.FlightSaleDto
import unq.pdes.flightsservice.controller.dtos.requests.FlightRequestDto
import unq.pdes.flightsservice.controller.dtos.requests.FlightSaleRequestDto
import unq.pdes.flightsservice.service.FlightService
import java.time.LocalDate

@RestController
class FlightController(
    private val flightService: FlightService,
) {

    @GetMapping("/flights")
    fun search(
        @RequestParam(required = false) origin: String?,
        @RequestParam(required = false) destination: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): ResponseEntity<List<FlightDto>> {
        val flights = flightService.search(origin, destination, date)
        return ResponseEntity.ok(flights.map { FlightDto.fromModel(it) })
    }

    @GetMapping("/flights/{id}")
    fun byId(@PathVariable id: Long): ResponseEntity<FlightDto> {
        return ResponseEntity.ok(FlightDto.fromModel(flightService.findById(id)))
    }

    @PostMapping("/flights")
    fun create(@Valid @RequestBody request: FlightRequestDto): ResponseEntity<FlightDto> {
        val flight = flightService.create(
            airline = request.airline,
            flightDate = request.flightDate,
            departureTime = request.departureTime,
            origin = request.origin,
            destination = request.destination,
            capacity = request.capacity,
            availability = request.availability,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(FlightDto.fromModel(flight))
    }

    @PostMapping("/flights/{id}/sales")
    fun sell(
        @PathVariable id: Long,
        @Valid @RequestBody request: FlightSaleRequestDto,
    ): ResponseEntity<FlightSaleDto> {
        val sale = flightService.sell(id, request.passengerName)
        return ResponseEntity.status(HttpStatus.CREATED).body(FlightSaleDto.fromModel(sale))
    }

    @DeleteMapping("/flights/sales/{id}")
    fun cancelSale(@PathVariable id: Long): ResponseEntity<Void> {
        flightService.cancelSale(id)
        return ResponseEntity.noContent().build()
    }
}
