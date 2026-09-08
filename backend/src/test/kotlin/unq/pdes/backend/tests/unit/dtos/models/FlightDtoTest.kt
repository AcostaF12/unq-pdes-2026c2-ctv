package unq.pdes.backend.tests.unit.dtos.models

import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.controller.dtos.models.FlightDto
import unq.pdes.backend.external.flights.ExternalFlightDto

@TestInstance(PER_CLASS)
class FlightDtoTest {

    @Test
    fun `01 - should map an external flight to FlightDto`() {
        val external = ExternalFlightDto(
            id = 7L,
            airline = "Aerolineas Argentinas",
            flightDate = LocalDate.of(2026, 12, 1),
            departureTime = LocalTime.of(8, 0),
            origin = "BUE",
            destination = "PAR",
            capacity = 180,
            availability = 42,
        )

        val dto = FlightDto.fromExternal(external)

        assertEquals(7L, dto.id)
        assertEquals("Aerolineas Argentinas", dto.airline)
        assertEquals(LocalDate.of(2026, 12, 1), dto.flightDate)
        assertEquals(LocalTime.of(8, 0), dto.departureTime)
        assertEquals("BUE", dto.origin)
        assertEquals("PAR", dto.destination)
        assertEquals(180, dto.capacity)
        assertEquals(42, dto.availability)
    }
}
