package unq.pdes.backend.tests.unit.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel

@TestInstance(PER_CLASS)
class HotelTest {

    private val city = City("BUE", "Buenos Aires")

    private fun validBuilder(): Hotel.Builder {
        return Hotel.Builder()
            .name("Alvear Palace Hotel")
            .city(city)
            .photoUrl("https://images.ctv.demo/hotels/alvear.jpg")
    }

    @Test
    fun `01 - builder should create a valid hotel`() {
        val hotel = validBuilder().id(1L).build()

        assertEquals(1L, hotel.id)
        assertEquals("Alvear Palace Hotel", hotel.name)
        assertEquals(city, hotel.city)
        assertEquals("https://images.ctv.demo/hotels/alvear.jpg", hotel.photoUrl)
    }

    @Test
    fun `02 - blank name should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Hotel.Builder().name("  ")
        }

        assertEquals("The hotel must have a name.", exception.message)
    }

    @Test
    fun `03 - blank photo url should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Hotel.Builder().photoUrl("")
        }

        assertEquals("The hotel must have a photo url.", exception.message)
    }

    @Test
    fun `04 - building without name should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Hotel.Builder().city(city).photoUrl("https://x.demo/p.jpg").build()
        }

        assertEquals("The hotel must have a name.", exception.message)
    }

    @Test
    fun `05 - building without city should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Hotel.Builder().name("Some hotel").photoUrl("https://x.demo/p.jpg").build()
        }

        assertEquals("The hotel must have a city.", exception.message)
    }

    @Test
    fun `06 - building without photo url should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Hotel.Builder().name("Some hotel").city(city).build()
        }

        assertEquals("The hotel must have a photo url.", exception.message)
    }

    @Test
    fun `07 - hotels with same id should be equal`() {
        val hotel1 = validBuilder().id(1L).build()
        val hotel2 = validBuilder().id(1L).name("Otro nombre").build()

        assertEquals(hotel1, hotel2)
        assertEquals(hotel1.hashCode(), hotel2.hashCode())
    }

    @Test
    fun `08 - hotels with different id should not be equal`() {
        val hotel1 = validBuilder().id(1L).build()
        val hotel2 = validBuilder().id(2L).build()

        assertNotEquals(hotel1, hotel2)
    }

    @Test
    fun `09 - hotel should not be equal to other object type`() {
        val hotel = validBuilder().id(1L).build()

        assertNotEquals(hotel, "Not a hotel")
    }

    @Test
    fun `10 - hotel hashcode without id should be zero`() {
        val hotel = validBuilder().build()

        assertEquals(0, hotel.hashCode())
    }

    @Test
    fun `11 - toString should contain the name and city`() {
        val text = validBuilder().id(1L).build().toString()

        assertTrue(text.contains("name='Alvear Palace Hotel'"))
        assertTrue(text.contains("city=BUE"))
    }
}
