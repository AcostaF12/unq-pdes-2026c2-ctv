package unq.pdes.flightsservice.tests.unit.dtos.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.flightsservice.controller.dtos.models.FlightDto
import unq.pdes.flightsservice.model.Flight
import java.time.LocalDate
import java.time.LocalTime

@TestInstance(PER_CLASS)
class FlightDtoTest {

    private fun sampleFlight(id: Long?): Flight {
        return Flight.Builder()
            .id(id)
            .airline("Aerolineas Argentinas")
            .flightDate(LocalDate.of(2026, 12, 1))
            .departureTime(LocalTime.of(8, 0))
            .origin("BUE")
            .destination("PAR")
            .capacity(180)
            .availability(120)
            .build()
    }

    @Test
    fun `01 - should convert from model to dto`() {
        val dto = FlightDto.fromModel(sampleFlight(5L))

        assertEquals(5L, dto.id)
        assertEquals("Aerolineas Argentinas", dto.airline)
        assertEquals(LocalDate.of(2026, 12, 1), dto.flightDate)
        assertEquals(LocalTime.of(8, 0), dto.departureTime)
        assertEquals("BUE", dto.origin)
        assertEquals("PAR", dto.destination)
        assertEquals(180, dto.capacity)
        assertEquals(120, dto.availability)
    }

    @Test
    fun `02 - should convert from dto to model`() {
        val model = FlightDto.fromModel(sampleFlight(7L)).toModel()

        assertEquals(7L, model.id)
        assertEquals("BUE", model.origin)
        assertEquals("PAR", model.destination)
        assertEquals(120, model.availability)
    }

    @Test
    fun `03 - round trip model to dto to model should preserve data`() {
        val original = sampleFlight(9L)

        val rebuilt = FlightDto.fromModel(original).toModel()

        assertEquals(original.id, rebuilt.id)
        assertEquals(original.airline, rebuilt.airline)
        assertEquals(original.capacity, rebuilt.capacity)
        assertEquals(original.availability, rebuilt.availability)
    }

    @Test
    fun `04 - should support null id`() {
        val dto = FlightDto.fromModel(sampleFlight(null))

        assertNull(dto.id)
    }
}
