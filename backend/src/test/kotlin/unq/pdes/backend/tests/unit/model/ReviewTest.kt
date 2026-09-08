package unq.pdes.backend.tests.unit.model

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class ReviewTest {

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

    @Test
    fun `06 - reviews with same id should be equal`() {
        val review1 = validBuilder().id(1L).build()
        val review2 = validBuilder().id(1L).score(3).build()

        assertEquals(review1, review2)
        assertEquals(review1.hashCode(), review2.hashCode())
    }

    @Test
    fun `07 - reviews with different id should not be equal`() {
        assertNotEquals(validBuilder().id(1L).build(), validBuilder().id(2L).build())
    }

    @Test
    fun `08 - review should not be equal to other object type`() {
        assertNotEquals(validBuilder().id(1L).build(), "Not a review")
    }

    @Test
    fun `09 - review hashcode without id should be zero`() {
        assertEquals(0, validBuilder().build().hashCode())
    }

    @Test
    fun `10 - toString should contain the score`() {
        val review = validBuilder().id(1L).build()

        assertTrue(review.toString().contains("score=8"))
    }
}
