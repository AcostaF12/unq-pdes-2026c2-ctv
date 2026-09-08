package unq.pdes.backend.tests.unit.dtos.models

import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.controller.dtos.models.PurchaseDto
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class PurchaseDtoTest {

    @Test
    fun `01 - should convert from model to dto`() {
        val paris = City("PAR", "Paris")
        val agency = Agency.Builder().id(3L).name("Despegar").build()
        val travelPackage = TravelPackage.Builder()
            .id(8L)
            .agency(agency)
            .hotel(Hotel.Builder().id(1L).name("Hotel").city(paris).photoUrl("https://x.demo/h.jpg").build())
            .origin(City("BUE", "Buenos Aires"))
            .destination(paris)
            .name("París Romántico")
            .outboundFlightId(1L)
            .returnFlightId(2L)
            .price(BigDecimal("1500.00"))
            .build()
        val purchase = Purchase.Builder()
            .id(4L)
            .buyer(
                User.Builder()
                    .id(1L).username("buyer").password("secret").role(Role.BUYER)
                    .firstName("Bruno").lastName("Buyer").build(),
            )
            .travelPackage(travelPackage)
            .agency(agency)
            .purchasePrice(BigDecimal("1500.00"))
            .purchasedAt(LocalDateTime.of(2026, 8, 20, 12, 0))
            .build()

        val dto = PurchaseDto.fromModel(purchase)

        assertEquals(4L, dto.id)
        assertEquals(8L, dto.packageId)
        assertEquals("París Romántico", dto.packageName)
        assertEquals("Despegar", dto.agency.name)
        assertEquals(BigDecimal("1500.00"), dto.purchasePrice)
        assertEquals(LocalDateTime.of(2026, 8, 20, 12, 0), dto.purchasedAt)
    }
}
