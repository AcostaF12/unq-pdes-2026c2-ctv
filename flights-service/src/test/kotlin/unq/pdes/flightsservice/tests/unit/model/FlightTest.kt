package unq.pdes.flightsservice.tests.unit.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.flightsservice.model.Flight
import java.time.LocalDate
import java.time.LocalTime

@TestInstance(PER_CLASS)
class FlightTest {

    private fun validBuilder(): Flight.Builder {
        return Flight.Builder()
            .airline("Aerolineas Argentinas")
            .flightDate(LocalDate.of(2026, 12, 1))
            .departureTime(LocalTime.of(8, 0))
            .origin("BUE")
            .destination("PAR")
            .capacity(180)
            .availability(180)
    }

    @Test
    fun `01 - builder should create a valid flight`() {
        val flight = validBuilder().id(1L).build()

        assertEquals(1L, flight.id)
        assertEquals("Aerolineas Argentinas", flight.airline)
        assertEquals("BUE", flight.origin)
        assertEquals("PAR", flight.destination)
        assertEquals(180, flight.capacity)
        assertEquals(180, flight.availability)
    }

    @Test
    fun `02 - blank airline should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Flight.Builder().airline("  ")
        }

        assertEquals("The flight must have an airline.", exception.message)
    }

    @Test
    fun `03 - blank origin should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Flight.Builder().origin("")
        }

        assertEquals("The flight must have an origin.", exception.message)
    }

    @Test
    fun `04 - blank destination should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Flight.Builder().destination("")
        }

        assertEquals("The flight must have a destination.", exception.message)
    }

    @Test
    fun `05 - non positive capacity should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Flight.Builder().capacity(0)
        }

        assertEquals("The flight capacity must be greater than zero.", exception.message)
    }

    @Test
    fun `06 - negative availability should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Flight.Builder().availability(-1)
        }

        assertEquals("The flight availability cannot be negative.", exception.message)
    }

    @Test
    fun `07 - availability greater than capacity should throw on build`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            validBuilder().capacity(100).availability(150).build()
        }

        assertEquals("Availability cannot exceed capacity.", exception.message)
    }

    @Test
    fun `08 - same origin and destination should throw on build`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            validBuilder().origin("BUE").destination("BUE").build()
        }

        assertEquals("Origin and destination must be different.", exception.message)
    }

    @Test
    fun `09 - building without airline should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Flight.Builder()
                .flightDate(LocalDate.of(2026, 12, 1))
                .departureTime(LocalTime.of(8, 0))
                .origin("BUE")
                .destination("PAR")
                .capacity(180)
                .availability(180)
                .build()
        }

        assertEquals("The flight must have an airline.", exception.message)
    }

    @Test
    fun `10 - flights with same id should be equal`() {
        val flight1 = validBuilder().id(1L).build()
        val flight2 = validBuilder().id(1L).airline("Other").build()

        assertEquals(flight1, flight2)
        assertEquals(flight1.hashCode(), flight2.hashCode())
    }

    @Test
    fun `11 - flights with different id should not be equal`() {
        val flight1 = validBuilder().id(1L).build()
        val flight2 = validBuilder().id(2L).build()

        assertNotEquals(flight1, flight2)
    }

    @Test
    fun `12 - flight should not be equal to other object type`() {
        val flight = validBuilder().id(1L).build()

        assertNotEquals(flight, "Not a flight")
    }

    @Test
    fun `13 - flight hashcode without id should be zero`() {
        val flight = validBuilder().build()

        assertEquals(0, flight.hashCode())
    }
}
