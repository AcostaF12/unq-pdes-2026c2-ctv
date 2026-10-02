package unq.pdes.backend.tests.unit.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import unq.pdes.backend.search.TravelPackageSearchCriteria
import java.math.BigDecimal

class TravelPackageSearchCriteriaTest {
    @Test
    fun `normalizes text filters before querying`() {
        val criteria = TravelPackageSearchCriteria.from("  Paris  ", " bue ", " par ", null, null)

        assertEquals("Paris", criteria.name)
        assertEquals("BUE", criteria.origin)
        assertEquals("PAR", criteria.destination)
    }

    @Test
    fun `converts blank filters to absent filters`() {
        val criteria = TravelPackageSearchCriteria.from(" ", " ", " ", null, null)

        assertNull(criteria.name)
        assertNull(criteria.origin)
        assertNull(criteria.destination)
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun `rejects negative and inverted price ranges`() {
        val negativePrice =
            assertThrows(IllegalArgumentException::class.java) {
                TravelPackageSearchCriteria.from(null, null, null, BigDecimal("-1.00"), null)
            }
        val invertedRange =
            assertThrows(IllegalArgumentException::class.java) {
                TravelPackageSearchCriteria.from(null, null, null, BigDecimal("100.00"), BigDecimal("99.00"))
            }

        assertEquals("The minimum price cannot be negative.", negativePrice.message)
        assertEquals("The minimum price cannot be greater than the maximum price.", invertedRange.message)
    }
}
