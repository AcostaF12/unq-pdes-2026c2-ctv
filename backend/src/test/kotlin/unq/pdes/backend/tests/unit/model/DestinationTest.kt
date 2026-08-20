package unq.pdes.backend.tests.unit.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.Destination

@TestInstance(PER_CLASS)
class DestinationTest {

    @Test
    fun `01 - destinations with same code should be equal`() {
        val destination1 = Destination("BUE", "Buenos Aires")
        val destination2 = Destination("BUE", "Otra ciudad")

        assertEquals(destination1, destination2)
        assertEquals(destination1.hashCode(), destination2.hashCode())
    }

    @Test
    fun `02 - destinations with different code should not be equal`() {
        val destination1 = Destination("BUE", "Buenos Aires")
        val destination2 = Destination("PAR", "Buenos Aires")

        assertNotEquals(destination1, destination2)
    }

    @Test
    fun `03 - destination should be equal to itself`() {
        val destination = Destination("BUE", "Buenos Aires")

        assertEquals(destination, destination)
    }

    @Test
    fun `04 - destination should not be equal to other object type`() {
        val destination = Destination("BUE", "Buenos Aires")

        assertNotEquals(destination, "Not a destination")
    }

    @Test
    fun `05 - destination hashcode should match its code hashcode`() {
        val destination = Destination("BUE", "Buenos Aires")

        assertEquals("BUE".hashCode(), destination.hashCode())
    }
}
