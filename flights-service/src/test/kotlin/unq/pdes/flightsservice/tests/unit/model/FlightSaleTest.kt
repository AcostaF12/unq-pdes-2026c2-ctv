package unq.pdes.flightsservice.tests.unit.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.flightsservice.model.Flight
import unq.pdes.flightsservice.model.FlightSale

@TestInstance(PER_CLASS)
class FlightSaleTest {

    private val flight = Flight.Builder()
        .id(1L)
        .airline("Aerolineas")
        .flightDate(LocalDate.of(2026, 12, 1))
        .departureTime(LocalTime.of(10, 30))
        .origin("BUE")
        .destination("PAR")
        .capacity(100)
        .availability(100)
        .build()

    private fun validBuilder(): FlightSale.Builder {
        return FlightSale.Builder().flight(flight).passengerName("Bruno Buyer")
    }

    @Test
    fun `01 - builder should create a valid sale`() {
        val sale = validBuilder().id(1L).build()

        assertEquals(1L, sale.id)
        assertEquals(flight, sale.flight)
        assertEquals("Bruno Buyer", sale.passengerName)
    }

    @Test
    fun `02 - blank passenger name should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            FlightSale.Builder().passengerName("  ")
        }

        assertEquals("The sale must have a passenger name.", exception.message)
    }

    @Test
    fun `03 - building without flight should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            FlightSale.Builder().passengerName("Bruno").build()
        }

        assertEquals("The sale must reference a flight.", exception.message)
    }

    @Test
    fun `04 - sales with different id should not be equal`() {
        assertNotEquals(validBuilder().id(1L).build(), validBuilder().id(2L).build())
    }

    @Test
    fun `05 - sales with same id should be equal`() {
        val sale1 = validBuilder().id(1L).build()
        val sale2 = validBuilder().id(1L).passengerName("Otro Pasajero").build()

        assertEquals(sale1, sale2)
        assertEquals(sale1.hashCode(), sale2.hashCode())
    }

    @Test
    fun `06 - sale should not be equal to other object type`() {
        assertNotEquals(validBuilder().id(1L).build(), "Not a sale")
    }

    @Test
    fun `07 - sale hashcode without id should be zero`() {
        assertEquals(0, validBuilder().build().hashCode())
    }

    @Test
    fun `08 - toString should contain the passenger name`() {
        assertTrue(validBuilder().id(1L).build().toString().contains("passengerName='Bruno Buyer'"))
    }
}
