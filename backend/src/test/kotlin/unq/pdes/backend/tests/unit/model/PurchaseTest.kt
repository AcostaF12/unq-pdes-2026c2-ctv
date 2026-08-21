package unq.pdes.backend.tests.unit.model

import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.Destination
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class PurchaseTest {

    private val buyer = User.Builder()
        .id(1L).username("buyer").password("secret").role(Role.BUYER).firstName("Bruno").lastName("Buyer").build()

    private val travelPackage = TravelPackage.Builder()
        .id(1L)
        .agency(Agency.Builder().id(1L).name("Despegar").build())
        .hotel(Hotel.Builder().id(1L).name("Hotel").destination(Destination("BUE", "Buenos Aires")).photoUrl("https://x.demo/h.jpg").build())
        .name("Paquete").outboundFlightId(1L).returnFlightId(2L).price(BigDecimal("1000.00")).build()

    private fun validBuilder(): Purchase.Builder {
        return Purchase.Builder()
            .buyer(buyer)
            .travelPackage(travelPackage)
            .purchasePrice(BigDecimal("1000.00"))
            .purchasedAt(LocalDateTime.of(2026, 8, 20, 12, 0))
    }

    @Test
    fun `01 - builder should create a valid purchase`() {
        val purchase = validBuilder().id(1L).build()

        assertEquals(1L, purchase.id)
        assertEquals(buyer, purchase.buyer)
        assertEquals(travelPackage, purchase.travelPackage)
        assertEquals(BigDecimal("1000.00"), purchase.purchasePrice)
    }

    @Test
    fun `02 - non positive price should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Purchase.Builder().purchasePrice(BigDecimal.ZERO)
        }

        assertEquals("The purchase price must be greater than zero.", exception.message)
    }

    @Test
    fun `03 - building without timestamp should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Purchase.Builder().buyer(buyer).travelPackage(travelPackage).purchasePrice(BigDecimal.TEN).build()
        }

        assertEquals("The purchase must have a timestamp.", exception.message)
    }

    @Test
    fun `04 - purchases with different id should not be equal`() {
        assertNotEquals(validBuilder().id(1L).build(), validBuilder().id(2L).build())
    }
}
