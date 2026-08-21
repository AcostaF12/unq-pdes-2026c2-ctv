package unq.pdes.backend.tests.unit.model

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.Destination
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class ReviewTest {

    private val buyer = User.Builder()
        .id(1L).username("buyer").password("secret").role(Role.BUYER).firstName("Bruno").lastName("Buyer").build()

    private val travelPackage = TravelPackage.Builder()
        .id(1L)
        .agency(Agency.Builder().id(1L).name("Despegar").build())
        .hotel(Hotel.Builder().id(1L).name("Hotel").destination(Destination("BUE", "Buenos Aires")).photoUrl("https://x.demo/h.jpg").build())
        .name("Paquete").outboundFlightId(1L).returnFlightId(2L).price(BigDecimal("1000.00")).build()

    private fun validBuilder(): Review.Builder {
        return Review.Builder().buyer(buyer).travelPackage(travelPackage).score(8)
    }

    @Test
    fun `01 - builder should create a valid review`() {
        val review = validBuilder().id(1L).comment("Muy bueno").build()

        assertEquals(1L, review.id)
        assertEquals(8, review.score)
        assertEquals("Muy bueno", review.comment)
    }

    @Test
    fun `02 - comment should be optional`() {
        val review = validBuilder().build()

        assertNull(review.comment)
    }

    @Test
    fun `03 - score below 0 should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Review.Builder().score(-1)
        }

        assertEquals("The score must be between 0 and 10.", exception.message)
    }

    @Test
    fun `04 - score above 10 should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Review.Builder().score(11)
        }

        assertEquals("The score must be between 0 and 10.", exception.message)
    }

    @Test
    fun `05 - building without score should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Review.Builder().buyer(buyer).travelPackage(travelPackage).build()
        }

        assertEquals("The review must have a score.", exception.message)
    }
}
