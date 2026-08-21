package unq.pdes.flightsservice.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.flightsservice.model.FlightSale

interface FlightSaleRepository : JpaRepository<FlightSale, Long> {

    fun findByFlightId(flightId: Long): List<FlightSale>
}
