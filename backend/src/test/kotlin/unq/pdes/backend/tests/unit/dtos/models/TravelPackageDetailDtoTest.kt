package unq.pdes.backend.tests.unit.dtos.models

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.controller.dtos.models.TravelPackageDetailDto
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class TravelPackageDetailDtoTest {

    private val paris = City("PAR", "Paris")
    private val travelPackage = TravelPackage.Builder()
        .id(8L)
        .agency(Agency.Builder().id(1L).name("Despegar").build())
        .hotel(Hotel.Builder().id(1L).name("Gran Hotel").city(paris).photoUrl("https://x.demo/h.jpg").build())
        .origin(City("BUE", "Buenos Aires"))
        .destination(paris)
        .name("París Romántico")
        .outboundFlightId(1L)
        .returnFlightId(2L)
        .price(BigDecimal("1500.00"))
        .build()

    @Test
    fun `01 - should map package with flights and average score`() {
        val outbound = flight(1L, "BUE", "PAR")
        val returnFlight = flight(2L, "PAR", "BUE")
        val review = Review.Builder()
            .id(1L)
            .buyer(
                User.Builder()
                    .id(1L).username("buyer").password("secret").role(Role.BUYER)
                    .firstName("Bruno").lastName("Buyer").build(),
            )
            .travelPackage(travelPackage)
            .score(8)
            .build()

        val dto = TravelPackageDetailDto.from(travelPackage, outbound, returnFlight, listOf(review))

        assertEquals(8L, dto.id)
        assertEquals("París Romántico", dto.name)
        assertEquals("BUE", dto.origin.code)
        assertEquals("BUE", dto.outboundFlight?.origin)
        assertEquals("PAR", dto.returnFlight?.origin)
        assertEquals(1, dto.reviews.size)
        assertEquals(8.0, dto.averageScore)
    }

    @Test
    fun `02 - should keep flights and average score null when missing`() {
        val dto = TravelPackageDetailDto.from(travelPackage, null, null, emptyList())

        assertNull(dto.outboundFlight)
        assertNull(dto.returnFlight)
        assertEquals(0, dto.reviews.size)
        assertNull(dto.averageScore)
    }

    private fun flight(id: Long, origin: String, destination: String): ExternalFlightDto {
        return ExternalFlightDto(
            id = id,
            airline = "Aerolineas Argentinas",
            flightDate = LocalDate.of(2026, 12, 1),
            departureTime = LocalTime.of(8, 0),
            origin = origin,
            destination = destination,
            capacity = 180,
            availability = 180,
        )
    }
}
