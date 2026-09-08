package unq.pdes.backend.tests.unit.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.City

@TestInstance(PER_CLASS)
class CityTest {

    @Test
    fun `01 - cities with same code should be equal`() {
        val city1 = City("BUE", "Buenos Aires")
        val city2 = City("BUE", "Otra ciudad")

        assertEquals(city1, city2)
        assertEquals(city1.hashCode(), city2.hashCode())
    }

    @Test
    fun `02 - cities with different code should not be equal`() {
        val city1 = City("BUE", "Buenos Aires")
        val city2 = City("PAR", "Buenos Aires")

        assertNotEquals(city1, city2)
    }

    @Test
    fun `03 - city should be equal to itself`() {
        val city = City("BUE", "Buenos Aires")

        assertEquals(city, city)
    }

    @Test
    fun `04 - city should not be equal to other object type`() {
        val city = City("BUE", "Buenos Aires")

        assertNotEquals(city, "Not a city")
    }

    @Test
    fun `05 - city hashcode should match its code hashcode`() {
        val city = City("BUE", "Buenos Aires")

        assertEquals("BUE".hashCode(), city.hashCode())
    }
}
