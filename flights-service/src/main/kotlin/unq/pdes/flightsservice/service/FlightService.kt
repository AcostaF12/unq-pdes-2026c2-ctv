package unq.pdes.flightsservice.service

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.LockModeType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.flightsservice.model.Flight
import unq.pdes.flightsservice.model.FlightSale
import unq.pdes.flightsservice.persistence.jpa.FlightRepository
import unq.pdes.flightsservice.persistence.jpa.FlightSaleRepository
import java.time.LocalDate
import java.time.LocalTime

@Service
class FlightService(
    private val flightRepository: FlightRepository,
    private val flightSaleRepository: FlightSaleRepository,
    private val entityManager: EntityManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun findAll(): List<Flight> = flightRepository.findAll()

    @Transactional(readOnly = true)
    fun seeded(): List<Flight> = flightRepository.findAllBySeedKeyIsNotNull()

    @Transactional(readOnly = true)
    fun search(
        origin: String?,
        destination: String?,
        date: LocalDate?,
    ): List<Flight> = flightRepository.search(origin, destination, date)

    @Transactional(readOnly = true)
    fun findById(id: Long): Flight =
        flightRepository
            .findById(id)
            .orElseThrow { EntityNotFoundException("There is no Flight with id: $id.") }

    @Transactional
    fun create(
        airline: String,
        flightDate: LocalDate,
        departureTime: LocalTime,
        origin: String,
        destination: String,
        capacity: Int,
        availability: Int,
    ): Flight {
        val flight =
            Flight
                .Builder()
                .airline(airline)
                .flightDate(flightDate)
                .departureTime(departureTime)
                .origin(origin)
                .destination(destination)
                .capacity(capacity)
                .availability(availability)
                .build()
        return flightRepository.save(flight)
    }

    @Transactional
    fun save(flight: Flight): Flight = flightRepository.save(flight)

    @Transactional
    fun sell(
        flightId: Long,
        passengerName: String,
    ): FlightSale {
        val flight =
            flightRepository.findForUpdate(flightId)
                ?: throw EntityNotFoundException("There is no Flight with id: $flightId.")
        if (flight.availability <= 0) {
            logger.warn("Sale rejected for flight {}: no availability left", flightId)
            throw IllegalArgumentException("The flight has no availability.")
        }
        flight.availability = flight.availability - 1
        flightRepository.save(flight)
        val sale =
            flightSaleRepository.save(
                FlightSale
                    .Builder()
                    .flight(flight)
                    .passengerName(passengerName)
                    .build(),
            )
        logger.info("Sale {} registered for flight {}; remaining availability {}", sale.id, flightId, flight.availability)
        return sale
    }

    @Transactional
    fun cancelSale(saleId: Long) {
        val sale =
            flightSaleRepository.findForUpdate(saleId)
                ?: throw EntityNotFoundException("There is no FlightSale with id: $saleId.")
        val flightId = sale.flight.id ?: throw EntityNotFoundException("There is no Flight for sale: $saleId.")
        val flight =
            flightRepository.findForUpdate(flightId)
                ?: throw EntityNotFoundException("There is no Flight with id: $flightId.")
        // The sale already loaded the flight into this session. Refresh under the lock so two
        // cancellations of the same flight cannot overwrite the counter and drop a seat.
        entityManager.refresh(flight, LockModeType.PESSIMISTIC_WRITE)
        flight.availability = flight.availability + 1
        flightRepository.save(flight)
        flightSaleRepository.delete(sale)
        logger.info("Sale {} cancelled for flight {}; remaining availability {}", saleId, flight.id, flight.availability)
    }
}
