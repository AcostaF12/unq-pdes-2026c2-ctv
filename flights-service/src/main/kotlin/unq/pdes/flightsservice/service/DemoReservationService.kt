package unq.pdes.flightsservice.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.flightsservice.model.DemoSeatReservation
import unq.pdes.flightsservice.model.FlightSale
import unq.pdes.flightsservice.persistence.jpa.FlightRepository
import unq.pdes.flightsservice.persistence.jpa.FlightSaleRepository

@Service
class DemoReservationService(
    private val flights: FlightRepository,
    private val sales: FlightSaleRepository,
) {
    /** Stable keys make a retry after a backend rollback safe across services. */
    @Transactional
    fun reserve(requests: List<DemoSeatReservation>) {
        require(requests.size <= 200) { "Too many demo reservations." }
        require(requests.map { it.key }.distinct().size == requests.size) { "Duplicate demo reservation keys." }
        val locked =
            requests.map { it.flightId }.distinct().sorted().associateWith { id ->
                flights.findForUpdate(id) ?: throw EntityNotFoundException("There is no Flight with id: $id.")
            }
        requests.forEach { request ->
            require(request.key.startsWith("ctv-demo-v1:") && request.key.length <= 300) { "Invalid demo reservation key." }
            val existing = sales.findBySeedKey(request.key)
            if (existing != null) {
                require(existing.flight.id == request.flightId && existing.passengerName == request.passengerName) {
                    "The demo reservation key already refers to a different sale."
                }
            } else {
                val flight = locked.getValue(request.flightId)
                require(flight.availability > 0) { "The flight has no availability." }
                flight.availability -= 1
                sales.save(
                    FlightSale.Builder().flight(flight).passengerName(request.passengerName).build().apply {
                        seedKey = request.key
                    },
                )
            }
        }
    }
}
