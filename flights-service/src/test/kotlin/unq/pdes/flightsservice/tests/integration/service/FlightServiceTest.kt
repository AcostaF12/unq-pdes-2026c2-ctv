package unq.pdes.flightsservice.tests.integration.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import unq.pdes.flightsservice.helpers.factory.PersistentObjectsFactory
import unq.pdes.flightsservice.helpers.service.DataServiceH2
import unq.pdes.flightsservice.service.FlightService
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
class FlightServiceTest {

    @Autowired
    private lateinit var flightService: FlightService

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - create should return the flight with an id`() {
        val flight = flightService.create(
            "Aerolineas Argentinas", LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), "BUE", "PAR", 180, 180,
        )

        assertNotNull(flight.id)
        assertEquals("BUE", flight.origin)
        assertEquals("PAR", flight.destination)
        assertEquals(180, flight.availability)
    }

    @Test
    fun `02 - create with invalid data should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            flightService.create(
                "Aerolineas Argentinas", LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), "BUE", "BUE", 180, 180,
            )
        }

        assertEquals("Origin and destination must be different.", exception.message)
    }

    @Test
    fun `03 - findById should return the persisted flight`() {
        val persisted = factory.anyFlight()

        val found = flightService.findById(persisted.id!!)

        assertEquals(persisted.id, found.id)
    }

    @Test
    fun `04 - findById should throw EntityNotFoundException when not found`() {
        val exception = assertThrows(EntityNotFoundException::class.java) {
            flightService.findById(999L)
        }

        assertEquals("There is no Flight with id: 999.", exception.message)
    }

    @Test
    fun `05 - findAll should return all persisted flights`() {
        factory.flightFrom("BUE", "PAR")
        factory.flightFrom("BUE", "LON")

        assertEquals(2, flightService.findAll().size)
    }

    @Test
    fun `06 - search should only return flights with availability greater than zero`() {
        factory.flightWithAvailability(0)
        factory.flightWithAvailability(50)

        val result = flightService.search(null, null, null)

        assertEquals(1, result.size)
        assertEquals(50, result.first().availability)
    }

    @Test
    fun `07 - search should filter by destination`() {
        factory.flightFrom("BUE", "PAR")
        factory.flightFrom("BUE", "LON")

        val result = flightService.search(null, "LON", null)

        assertEquals(1, result.size)
        assertEquals("LON", result.first().destination)
    }

    @Test
    fun `08 - search should filter by origin`() {
        factory.flightFrom("BUE", "PAR")
        factory.flightFrom("ROM", "PAR")

        val result = flightService.search("ROM", null, null)

        assertEquals(1, result.size)
        assertEquals("ROM", result.first().origin)
    }

    @Test
    fun `09 - search should filter by date`() {
        val targetDate = LocalDate.of(2026, 12, 25)
        factory.flightOn(targetDate)
        factory.flightOn(LocalDate.of(2026, 12, 1))

        val result = flightService.search(null, null, targetDate)

        assertEquals(1, result.size)
        assertEquals(targetDate, result.first().flightDate)
    }

    @Test
    fun `10 - search without filters should return all available flights`() {
        factory.flightFrom("BUE", "PAR")
        factory.flightFrom("BUE", "LON")

        assertTrue(flightService.search(null, null, null).size == 2)
    }

    @Test
    fun `11 - sell should decrement availability and persist the sale`() {
        val flight = factory.flightWithAvailability(2)

        val sale = flightService.sell(flight.id!!, "Bruno Buyer")

        assertNotNull(sale.id)
        assertEquals("Bruno Buyer", sale.passengerName)
        assertEquals(1, flightService.findById(flight.id!!).availability)
    }

    @Test
    fun `12 - sell without availability should throw IllegalArgumentException`() {
        val flight = factory.flightWithAvailability(0)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            flightService.sell(flight.id!!, "Bruno Buyer")
        }

        assertEquals("The flight has no availability.", exception.message)
    }

    @Test
    fun `13 - cancelSale should restore availability`() {
        val flight = factory.flightWithAvailability(1)
        val sale = flightService.sell(flight.id!!, "Bruno Buyer")

        flightService.cancelSale(sale.id!!)

        assertEquals(1, flightService.findById(flight.id!!).availability)
    }
}
