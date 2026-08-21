package unq.pdes.backend.tests.unit.dtos.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.controller.dtos.models.DestinationDto
import unq.pdes.backend.controller.dtos.models.HotelDto
import unq.pdes.backend.model.Destination
import unq.pdes.backend.model.Hotel

@TestInstance(PER_CLASS)
class HotelDtoTest {

    private val destination = Destination("BUE", "Buenos Aires")

    @Test
    fun `01 - should create a HotelDto with all parameters`() {
        val dto = HotelDto(
            id = 1L,
            name = "Alvear Palace Hotel",
            destination = DestinationDto.fromModel(destination),
            photoUrl = "https://x.demo/p.jpg",
        )

        assertEquals(1L, dto.id)
        assertEquals("Alvear Palace Hotel", dto.name)
        assertEquals("BUE", dto.destination.code)
        assertEquals("https://x.demo/p.jpg", dto.photoUrl)
    }

    @Test
    fun `02 - should convert from model to dto`() {
        val hotel = Hotel.Builder()
            .id(5L)
            .name("The Savoy")
            .destination(destination)
            .photoUrl("https://x.demo/savoy.jpg")
            .build()

        val dto = HotelDto.fromModel(hotel)

        assertEquals(5L, dto.id)
        assertEquals("The Savoy", dto.name)
        assertEquals("BUE", dto.destination.code)
        assertEquals("https://x.demo/savoy.jpg", dto.photoUrl)
    }

    @Test
    fun `03 - should convert from dto to model`() {
        val dto = HotelDto(
            id = 7L,
            name = "Copacabana Palace",
            destination = DestinationDto("RIO", "Rio de Janeiro"),
            photoUrl = "https://x.demo/copa.jpg",
        )

        val model = dto.toModel()

        assertEquals(7L, model.id)
        assertEquals("Copacabana Palace", model.name)
        assertEquals("RIO", model.destination.code)
        assertEquals("https://x.demo/copa.jpg", model.photoUrl)
    }

    @Test
    fun `04 - should create a HotelDto with null id`() {
        val dto = HotelDto(
            id = null,
            name = "Nuevo Hotel",
            destination = DestinationDto.fromModel(destination),
            photoUrl = "https://x.demo/p.jpg",
        )

        assertNull(dto.id)
    }
}
