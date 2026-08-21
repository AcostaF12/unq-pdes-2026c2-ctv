package unq.pdes.backend.tests.unit.model

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.Destination
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.TravelPackage

@TestInstance(PER_CLASS)
class TravelPackageTest {

    private val agency = Agency.Builder().id(1L).name("Despegar").build()
    private val hotel = Hotel.Builder()
        .id(1L).name("Gran Hotel").destination(Destination("BUE", "Buenos Aires")).photoUrl("https://x.demo/h.jpg").build()

    private fun validBuilder(): TravelPackage.Builder {
        return TravelPackage.Builder()
            .agency(agency)
            .hotel(hotel)
            .name("Escapada a Buenos Aires")
            .outboundFlightId(10L)
            .returnFlightId(20L)
            .price(BigDecimal("1500.00"))
    }

    @Test
    fun `01 - builder should create a valid package`() {
        val travelPackage = validBuilder().id(1L).build()

        assertEquals(1L, travelPackage.id)
        assertEquals("Escapada a Buenos Aires", travelPackage.name)
        assertEquals(agency, travelPackage.agency)
        assertEquals(hotel, travelPackage.hotel)
        assertEquals(10L, travelPackage.outboundFlightId)
        assertEquals(20L, travelPackage.returnFlightId)
        assertEquals(BigDecimal("1500.00"), travelPackage.price)
    }

    @Test
    fun `02 - blank name should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            TravelPackage.Builder().name("  ")
        }

        assertEquals("The package must have a name.", exception.message)
    }

    @Test
    fun `03 - non positive price should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            TravelPackage.Builder().price(BigDecimal.ZERO)
        }

        assertEquals("The package price must be greater than zero.", exception.message)
    }

    @Test
    fun `04 - same outbound and return flight should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            validBuilder().outboundFlightId(5L).returnFlightId(5L).build()
        }

        assertEquals("Outbound and return flights must be different.", exception.message)
    }

    @Test
    fun `05 - building without agency should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            TravelPackage.Builder().hotel(hotel).name("X").outboundFlightId(1L).returnFlightId(2L).price(BigDecimal.TEN).build()
        }

        assertEquals("The package must belong to an agency.", exception.message)
    }

    @Test
    fun `06 - packages with same id should be equal`() {
        val package1 = validBuilder().id(1L).build()
        val package2 = validBuilder().id(1L).name("Otro nombre").build()

        assertEquals(package1, package2)
        assertEquals(package1.hashCode(), package2.hashCode())
    }

    @Test
    fun `07 - packages with different id should not be equal`() {
        assertNotEquals(validBuilder().id(1L).build(), validBuilder().id(2L).build())
    }
}
