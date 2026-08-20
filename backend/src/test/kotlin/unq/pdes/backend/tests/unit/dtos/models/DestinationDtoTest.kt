package unq.pdes.backend.tests.unit.dtos.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.controller.dtos.models.DestinationDto
import unq.pdes.backend.model.Destination

@TestInstance(PER_CLASS)
class DestinationDtoTest {

    @Test
    fun `01 - should create a DestinationDto with all parameters`() {
        val dto = DestinationDto(code = "BUE", city = "Buenos Aires")

        assertEquals("BUE", dto.code)
        assertEquals("Buenos Aires", dto.city)
    }

    @Test
    fun `02 - should convert from model to dto`() {
        val dto = DestinationDto.fromModel(Destination("PAR", "Paris"))

        assertEquals("PAR", dto.code)
        assertEquals("Paris", dto.city)
    }

    @Test
    fun `03 - should convert from dto to model`() {
        val model = DestinationDto("LON", "London").toModel()

        assertEquals("LON", model.code)
        assertEquals("London", model.city)
    }

    @Test
    fun `04 - should compare equal DTOs correctly`() {
        assertEquals(DestinationDto("BUE", "Buenos Aires"), DestinationDto("BUE", "Buenos Aires"))
    }
}
