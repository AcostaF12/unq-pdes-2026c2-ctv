package unq.pdes.backend.tests.integration.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.service.HotelService

@SpringBootTest
class HotelServiceTest {

    @Autowired
    private lateinit var hotelService: HotelService

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - create should return the hotel with an id`() {
        factory.cityWith("BUE", "Buenos Aires")

        val hotel = hotelService.create("Alvear Palace Hotel", "BUE", "https://x.demo/p.jpg")

        assertNotNull(hotel.id)
        assertEquals("Alvear Palace Hotel", hotel.name)
        assertEquals("BUE", hotel.city.code)
    }

    @Test
    fun `02 - create with unknown city should throw EntityNotFoundException`() {
        val exception = assertThrows(EntityNotFoundException::class.java) {
            hotelService.create("Some hotel", "ZZZ", "https://x.demo/p.jpg")
        }

        assertEquals("There is no City with code: ZZZ.", exception.message)
    }

    @Test
    fun `03 - findById should return the persisted hotel`() {
        val persisted = factory.anyHotel()

        val found = hotelService.findById(persisted.id!!)

        assertEquals(persisted.id, found.id)
        assertEquals(persisted.name, found.name)
    }

    @Test
    fun `04 - findById should throw EntityNotFoundException when not found`() {
        val exception = assertThrows(EntityNotFoundException::class.java) {
            hotelService.findById(999L)
        }

        assertEquals("There is no Hotel with id: 999.", exception.message)
    }

    @Test
    fun `05 - findAll should return all persisted hotels`() {
        factory.hotelNamed("Hotel A")
        factory.hotelNamed("Hotel B")

        assertEquals(2, hotelService.findAll().size)
    }

    @Test
    fun `06 - update should modify the hotel keeping its id`() {
        val persisted = factory.anyHotel()
        factory.cityWith("PAR", "Paris")

        val updated = hotelService.update(persisted.id!!, "Hotel Le Meurice", "PAR", "https://x.demo/lm.jpg")

        assertEquals(persisted.id, updated.id)
        assertEquals("Hotel Le Meurice", updated.name)
        assertEquals("PAR", updated.city.code)
    }

    @Test
    fun `07 - update should throw EntityNotFoundException when hotel not found`() {
        factory.cityWith("BUE", "Buenos Aires")

        val exception = assertThrows(EntityNotFoundException::class.java) {
            hotelService.update(999L, "Some hotel", "BUE", "https://x.demo/p.jpg")
        }

        assertEquals("There is no Hotel with id: 999.", exception.message)
    }

    @Test
    fun `08 - delete should remove the hotel`() {
        val persisted = factory.anyHotel()

        hotelService.deleteById(persisted.id!!)

        assertTrue(hotelService.findAll().isEmpty())
    }

    @Test
    fun `09 - delete should throw EntityNotFoundException when hotel not found`() {
        val exception = assertThrows(EntityNotFoundException::class.java) {
            hotelService.deleteById(999L)
        }

        assertEquals("There is no Hotel with id: 999.", exception.message)
    }
}
