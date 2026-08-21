package unq.pdes.backend.tests.integration.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Destination
import unq.pdes.backend.service.DestinationService

@SpringBootTest
class DestinationServiceTest {

    @Autowired
    private lateinit var destinationService: DestinationService

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - save should persist the destination`() {
        val destination = destinationService.save(Destination("BUE", "Buenos Aires"))

        assertEquals("BUE", destination.code)
        assertEquals("Buenos Aires", destination.city)
    }

    @Test
    fun `02 - findByCode should return the destination when it exists`() {
        destinationService.save(Destination("PAR", "Paris"))

        val found = destinationService.findByCode("PAR")

        assertEquals("Paris", found.city)
    }

    @Test
    fun `03 - findByCode should throw EntityNotFoundException when not found`() {
        val exception = assertThrows(EntityNotFoundException::class.java) {
            destinationService.findByCode("ZZZ")
        }

        assertEquals("There is no Destination with code: ZZZ.", exception.message)
    }

    @Test
    fun `04 - existsByCode should return true when the destination exists`() {
        destinationService.save(Destination("LON", "London"))

        assertTrue(destinationService.existsByCode("LON"))
    }

    @Test
    fun `05 - existsByCode should return false when the destination does not exist`() {
        assertFalse(destinationService.existsByCode("ZZZ"))
    }

    @Test
    fun `06 - findAll should return all persisted destinations`() {
        destinationService.save(Destination("BUE", "Buenos Aires"))
        destinationService.save(Destination("PAR", "Paris"))

        assertEquals(2, destinationService.findAll().size)
    }
}
