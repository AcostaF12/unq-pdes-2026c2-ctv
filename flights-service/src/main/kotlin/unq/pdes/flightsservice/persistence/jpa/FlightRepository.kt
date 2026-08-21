package unq.pdes.flightsservice.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import unq.pdes.flightsservice.model.Flight
import java.time.LocalDate

interface FlightRepository : JpaRepository<Flight, Long> {

    @Query(
        """
        SELECT f FROM Flight f
        WHERE f.availability > 0
        AND (:origin IS NULL OR f.origin = :origin)
        AND (:destination IS NULL OR f.destination = :destination)
        AND (:date IS NULL OR f.flightDate = :date)
        ORDER BY f.flightDate, f.departureTime
        """,
    )
    fun search(
        @Param("origin") origin: String?,
        @Param("destination") destination: String?,
        @Param("date") date: LocalDate?,
    ): List<Flight>
}
