package unq.pdes.backend.service

import org.springframework.stereotype.Service
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.external.flights.ExternalFlightRequestDto
import unq.pdes.backend.external.flights.FlightsClient
import java.time.LocalDate
import java.time.LocalTime

@Service
class FlightService(
    private val flightsClient: FlightsClient,
    private val cityService: CityService,
) {
    fun search(
        origin: String?,
        destination: String?,
        date: LocalDate?,
    ): List<ExternalFlightDto> = flightsClient.search(normalizeCode(origin), normalizeCode(destination), date)

    fun findById(id: Long): ExternalFlightDto = flightsClient.findById(id)

    fun create(
        airline: String,
        flightDate: LocalDate,
        departureTime: LocalTime,
        origin: String,
        destination: String,
        capacity: Int,
        availability: Int,
    ): ExternalFlightDto {
        val originCode = origin.trim().uppercase()
        val destinationCode = destination.trim().uppercase()
        require(cityService.existsByCode(originCode)) { "There is no City with code: $originCode." }
        require(cityService.existsByCode(destinationCode)) { "There is no City with code: $destinationCode." }
        require(originCode != destinationCode) { "Origin and destination must be different." }
        require(availability <= capacity) { "Availability cannot exceed capacity." }
        return flightsClient.create(
            ExternalFlightRequestDto(
                airline = airline.trim(),
                flightDate = flightDate,
                departureTime = departureTime,
                origin = originCode,
                destination = destinationCode,
                capacity = capacity,
                availability = availability,
            ),
        )
    }

    private fun normalizeCode(code: String?): String? = code?.trim()?.takeIf { it.isNotEmpty() }?.uppercase()
}
