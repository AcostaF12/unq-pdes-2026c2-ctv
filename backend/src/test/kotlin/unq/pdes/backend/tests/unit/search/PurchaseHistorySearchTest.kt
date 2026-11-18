package unq.pdes.backend.tests.unit.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import unq.pdes.backend.search.PurchaseHistorySearchCriteria
import unq.pdes.backend.search.PurchaseHistorySort
import java.math.BigDecimal
import java.time.LocalDate

class PurchaseHistorySearchTest {
    @Test
    fun `criteria should normalize text filters and drop blank ones`() {
        val criteria = PurchaseHistorySearchCriteria.from("  Paris ", " par ", null, null, null, null)
        val blank = PurchaseHistorySearchCriteria.from(" ", " ", null, null, null, null)

        assertEquals("Paris", criteria.packageName)
        assertEquals("PAR", criteria.destination)
        assertNull(blank.packageName)
        assertNull(blank.destination)
    }

    @Test
    fun `criteria should reject negative prices`() {
        val min =
            assertThrows(IllegalArgumentException::class.java) {
                PurchaseHistorySearchCriteria.from(null, null, null, null, BigDecimal("-1"), null)
            }
        val max =
            assertThrows(IllegalArgumentException::class.java) {
                PurchaseHistorySearchCriteria.from(null, null, null, null, null, BigDecimal("-1"))
            }

        assertEquals("The minimum price cannot be negative.", min.message)
        assertEquals("The maximum price cannot be negative.", max.message)
    }

    @Test
    fun `criteria should accept equal bounds`() {
        val day = LocalDate.of(2026, 3, 1)

        val criteria = PurchaseHistorySearchCriteria.from(null, null, day, day, BigDecimal.TEN, BigDecimal.TEN)

        assertEquals(day, criteria.from)
        assertEquals(BigDecimal.TEN, criteria.maxPrice)
    }

    @Test
    fun `sort should default to the newest purchase first`() {
        val expected = Sort.by(Sort.Order.desc("purchasedAt"), Sort.Order.desc("id"))

        assertEquals(expected, PurchaseHistorySort.parse(null))
        assertEquals(expected, PurchaseHistorySort.parse("  "))
        assertEquals(expected, PurchaseHistorySort.parse("DATE,DESC"))
    }

    @Test
    fun `sort should map every supported option`() {
        assertEquals(Sort.by(Sort.Order.asc("purchasedAt"), Sort.Order.desc("id")), PurchaseHistorySort.parse("date,asc"))
        assertEquals(Sort.by(Sort.Order.asc("purchasePrice"), Sort.Order.desc("id")), PurchaseHistorySort.parse("price,asc"))
        assertEquals(Sort.by(Sort.Order.desc("purchasePrice"), Sort.Order.desc("id")), PurchaseHistorySort.parse("price,desc"))
    }

    @Test
    fun `sort should reject unsupported options`() {
        val exception = assertThrows(IllegalArgumentException::class.java) { PurchaseHistorySort.parse("agency,asc") }

        assertEquals("Cannot sort purchases by 'agency,asc'.", exception.message)
    }
}
