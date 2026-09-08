package unq.pdes.backend.tests.unit.dtos.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.controller.dtos.models.CityDto
import unq.pdes.backend.model.City

@TestInstance(PER_CLASS)
class CityDtoTest {

    @Test
    fun `01 - should create a CityDto with all parameters`() {
        val dto = CityDto(code = "BUE", name = "Buenos Aires")

        assertEquals("BUE", dto.code)
        assertEquals("Buenos Aires", dto.name)
    }

    @Test
    fun `02 - should convert from model to dto`() {
        val dto = CityDto.fromModel(City("PAR", "Paris"))

        assertEquals("PAR", dto.code)
        assertEquals("Paris", dto.name)
    }

    @Test
    fun `03 - should convert from dto to model`() {
        val model = CityDto("LON", "London").toModel()

        assertEquals("LON", model.code)
        assertEquals("London", model.name)
    }

    @Test
    fun `04 - should compare equal DTOs correctly`() {
        assertEquals(CityDto("BUE", "Buenos Aires"), CityDto("BUE", "Buenos Aires"))
    }
}
