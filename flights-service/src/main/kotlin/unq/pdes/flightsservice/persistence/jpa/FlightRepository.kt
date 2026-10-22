package unq.pdes.flightsservice.persistence.jpa

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import unq.pdes.flightsservice.model.Flight
import java.time.LocalDate

interface FlightRepository : JpaRepository<Flight, Long> {
    fun findAllBySeedKeyIsNotNull(): List<Flight>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Flight f where f.id = :id")
    fun findForUpdate(
        @Param("id") id: Long,
    ): Flight?

    @Query(
        """
        SELECT f FROM Flight f
        WHERE f.availability > 0
        AND (:origin IS NULL OR f.origin = :origin)
        AND (:destination IS NULL OR f.destination = :destination)
        AND (CAST(:date AS date) IS NULL OR f.flightDate = :date)
        ORDER BY f.flightDate, f.departureTime
        """,
    )
    fun search(
        @Param("origin") origin: String?,
        @Param("destination") destination: String?,
        @Param("date") date: LocalDate?,
    ): List<Flight>
}
