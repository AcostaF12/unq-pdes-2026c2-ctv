package unq.pdes.backend.tests.unit.model

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Favorite
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class FavoriteTest {

    private val buyer = User.Builder()
        .id(1L).username("buyer").password("secret").role(Role.BUYER).firstName("Bruno").lastName("Buyer").build()

    private val paris = City("PAR", "Paris")
    private val travelPackage = TravelPackage.Builder()
        .id(1L)
        .agency(Agency.Builder().id(1L).name("Despegar").build())
        .hotel(Hotel.Builder().id(1L).name("Hotel").city(paris).photoUrl("https://x.demo/h.jpg").build())
        .origin(City("BUE", "Buenos Aires"))
        .destination(paris)
        .name("Paquete").outboundFlightId(1L).returnFlightId(2L).price(BigDecimal("1000.00")).build()

    private fun validBuilder(): Favorite.Builder {
        return Favorite.Builder().buyer(buyer).travelPackage(travelPackage)
    }

    @Test
    fun `01 - builder should create a valid favorite`() {
        val favorite = validBuilder().id(1L).build()

        assertEquals(1L, favorite.id)
        assertEquals(buyer, favorite.buyer)
        assertEquals(travelPackage, favorite.travelPackage)
    }

    @Test
    fun `02 - building without buyer should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Favorite.Builder().travelPackage(travelPackage).build()
        }

        assertEquals("The favorite must have a buyer.", exception.message)
    }

    @Test
    fun `03 - building without package should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Favorite.Builder().buyer(buyer).build()
        }

        assertEquals("The favorite must reference a package.", exception.message)
    }

    @Test
    fun `04 - favorites with same id should be equal`() {
        assertEquals(validBuilder().id(1L).build(), validBuilder().id(1L).build())
    }

    @Test
    fun `05 - favorites with different id should not be equal`() {
        assertNotEquals(validBuilder().id(1L).build(), validBuilder().id(2L).build())
    }

    @Test
    fun `06 - favorite should not be equal to other object type`() {
        assertNotEquals(validBuilder().id(1L).build(), "Not a favorite")
    }

    @Test
    fun `07 - favorite hashcode without id should be zero`() {
        assertEquals(0, validBuilder().build().hashCode())
    }

    @Test
    fun `08 - toString should contain the buyer and package`() {
        assertTrue(validBuilder().id(1L).build().toString().contains("buyer=1"))
    }
}
