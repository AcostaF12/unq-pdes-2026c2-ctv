package unq.pdes.flightsservice.persistence.jpa

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import unq.pdes.flightsservice.model.FlightSale

interface FlightSaleRepository : JpaRepository<FlightSale, Long> {
    fun findBySeedKey(seedKey: String): FlightSale?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FlightSale s where s.id = :id")
    fun findForUpdate(id: Long): FlightSale?

    fun findByFlightId(flightId: Long): List<FlightSale>
}
