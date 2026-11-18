package unq.pdes.flightsservice.controller

import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import unq.pdes.flightsservice.controller.dtos.models.FlightDto
import unq.pdes.flightsservice.controller.dtos.requests.DemoReservationsRequestDto
import unq.pdes.flightsservice.model.DemoSeatReservation
import unq.pdes.flightsservice.service.DemoReservationService
import unq.pdes.flightsservice.service.FlightService

/** Internal demo-only API. The main backend never exposes it to application users. */
@RestController
@ConditionalOnProperty(prefix = "app.seed", name = ["enabled"], havingValue = "true")
class DemoSeedController(
    private val flights: FlightService,
    private val reservations: DemoReservationService,
) {
    @GetMapping("/internal/demo/flights")
    fun catalog(): List<FlightDto> = flights.seeded().map(FlightDto::fromModel)

    @PostMapping("/internal/demo/reservations")
    fun reserve(
        @Valid @RequestBody request: DemoReservationsRequestDto,
    ): ResponseEntity<Void> {
        reservations.reserve(request.reservations.map { DemoSeatReservation(it.key, it.flightId, it.passengerName) })
        return ResponseEntity.noContent().build()
    }
}
