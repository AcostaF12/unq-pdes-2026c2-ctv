package unq.pdes.backend.tests.unit.dtos.models

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.controller.dtos.models.FavoriteDto
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Favorite
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class FavoriteDtoTest {

    @Test
    fun `01 - should convert from model to dto`() {
        val paris = City("PAR", "Paris")
        val travelPackage = TravelPackage.Builder()
            .id(8L)
            .agency(Agency.Builder().id(1L).name("Despegar").build())
            .hotel(Hotel.Builder().id(1L).name("Hotel").city(paris).photoUrl("https://x.demo/h.jpg").build())
            .origin(City("BUE", "Buenos Aires"))
            .destination(paris)
            .name("París Romántico")
            .outboundFlightId(1L)
            .returnFlightId(2L)
            .price(BigDecimal("1500.00"))
            .build()
        val favorite = Favorite.Builder()
            .id(5L)
            .buyer(
                User.Builder()
                    .id(1L).username("buyer").password("secret").role(Role.BUYER)
                    .firstName("Bruno").lastName("Buyer").build(),
            )
            .travelPackage(travelPackage)
            .build()

        val dto = FavoriteDto.fromModel(favorite)

        assertEquals(5L, dto.id)
        assertEquals(8L, dto.packageId)
        assertEquals("París Romántico", dto.travelPackage.name)
    }
}
