package unq.pdes.flightsservice.service

import jakarta.persistence.EntityNotFoundException
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
) {

    @Transactional(readOnly = true)
    fun findAll(): List<Flight> {
        return flightRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun search(origin: String?, destination: String?, date: LocalDate?): List<Flight> {
        return flightRepository.search(origin, destination, date)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Flight {
        return flightRepository.findById(id)
            .orElseThrow { EntityNotFoundException("There is no Flight with id: $id.") }
    }

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
        val flight = Flight.Builder()
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
    fun save(flight: Flight): Flight {
        return flightRepository.save(flight)
    }

    @Transactional
    fun sell(flightId: Long, passengerName: String): FlightSale {
        val flight = findById(flightId)
        require(flight.availability > 0) { "The flight has no availability." }
        flight.availability = flight.availability - 1
        flightRepository.save(flight)
        return flightSaleRepository.save(
            FlightSale.Builder()
                .flight(flight)
                .passengerName(passengerName)
                .build(),
        )
    }

    @Transactional
    fun cancelSale(saleId: Long) {
        val sale = flightSaleRepository.findById(saleId)
            .orElseThrow { EntityNotFoundException("There is no FlightSale with id: $saleId.") }
        val flight = sale.flight
        flight.availability = flight.availability + 1
        flightRepository.save(flight)
        flightSaleRepository.delete(sale)
    }
}
