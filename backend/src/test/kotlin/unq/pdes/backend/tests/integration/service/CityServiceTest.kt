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
import unq.pdes.backend.model.City
import unq.pdes.backend.service.CityService

@SpringBootTest
class CityServiceTest {

    @Autowired
    private lateinit var cityService: CityService

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - save should persist the city`() {
        val city = cityService.save(City("BUE", "Buenos Aires"))

        assertEquals("BUE", city.code)
        assertEquals("Buenos Aires", city.name)
    }

    @Test
    fun `02 - findByCode should return the city when it exists`() {
        cityService.save(City("PAR", "Paris"))

        val found = cityService.findByCode("PAR")

        assertEquals("Paris", found.name)
    }

    @Test
    fun `03 - findByCode should throw EntityNotFoundException when not found`() {
        val exception = assertThrows(EntityNotFoundException::class.java) {
            cityService.findByCode("ZZZ")
        }

        assertEquals("There is no City with code: ZZZ.", exception.message)
    }

    @Test
    fun `04 - existsByCode should return true when the city exists`() {
        cityService.save(City("LON", "London"))

        assertTrue(cityService.existsByCode("LON"))
    }

    @Test
    fun `05 - existsByCode should return false when the city does not exist`() {
        assertFalse(cityService.existsByCode("ZZZ"))
    }

    @Test
    fun `06 - findAll should return all persisted cities`() {
        cityService.save(City("BUE", "Buenos Aires"))
        cityService.save(City("PAR", "Paris"))

        assertEquals(2, cityService.findAll().size)
    }
}
