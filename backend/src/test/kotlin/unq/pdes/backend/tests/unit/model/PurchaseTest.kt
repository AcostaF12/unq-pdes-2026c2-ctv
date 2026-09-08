package unq.pdes.backend.tests.unit.model

import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class PurchaseTest {

    private val buyer = User.Builder()
        .id(1L).username("buyer").password("secret").role(Role.BUYER).firstName("Bruno").lastName("Buyer").build()

    private val agency = Agency.Builder().id(1L).name("Despegar").build()
    private val paris = City("PAR", "Paris")
    private val travelPackage = TravelPackage.Builder()
        .id(1L)
        .agency(agency)
        .hotel(Hotel.Builder().id(1L).name("Hotel").city(paris).photoUrl("https://x.demo/h.jpg").build())
        .origin(City("BUE", "Buenos Aires"))
        .destination(paris)
        .name("Paquete").outboundFlightId(1L).returnFlightId(2L).price(BigDecimal("1000.00")).build()

    private fun validBuilder(): Purchase.Builder {
        return Purchase.Builder()
            .buyer(buyer)
            .travelPackage(travelPackage)
            .agency(agency)
            .purchasePrice(BigDecimal("1000.00"))
            .purchasedAt(LocalDateTime.of(2026, 8, 20, 12, 0))
    }

    @Test
    fun `01 - builder should create a valid purchase`() {
        val purchase = validBuilder().id(1L).build()

        assertEquals(1L, purchase.id)
        assertEquals(buyer, purchase.buyer)
        assertEquals(travelPackage, purchase.travelPackage)
        assertEquals(agency, purchase.agency)
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
            Purchase.Builder().buyer(buyer).travelPackage(travelPackage).agency(agency)
                .purchasePrice(BigDecimal.TEN).build()
        }

        assertEquals("The purchase must have a timestamp.", exception.message)
    }

    @Test
    fun `04 - purchases with different id should not be equal`() {
        assertNotEquals(validBuilder().id(1L).build(), validBuilder().id(2L).build())
    }

    @Test
    fun `05 - purchases with same id should be equal`() {
        val purchase1 = validBuilder().id(1L).build()
        val purchase2 = validBuilder().id(1L).purchasePrice(BigDecimal("2000.00")).build()

        assertEquals(purchase1, purchase2)
        assertEquals(purchase1.hashCode(), purchase2.hashCode())
    }

    @Test
    fun `06 - purchase should not be equal to other object type`() {
        assertNotEquals(validBuilder().id(1L).build(), "Not a purchase")
    }

    @Test
    fun `07 - purchase hashcode without id should be zero`() {
        assertEquals(0, validBuilder().build().hashCode())
    }

    @Test
    fun `08 - toString should contain the price`() {
        assertTrue(validBuilder().id(1L).build().toString().contains("price=1000.00"))
    }

    @Test
    fun `09 - building without agency should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Purchase.Builder().buyer(buyer).travelPackage(travelPackage)
                .purchasePrice(BigDecimal.TEN)
                .purchasedAt(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build()
        }

        assertEquals("The purchase must belong to an agency.", exception.message)
    }
}
