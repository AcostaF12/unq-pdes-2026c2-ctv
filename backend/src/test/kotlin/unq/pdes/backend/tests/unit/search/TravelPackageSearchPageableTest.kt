package unq.pdes.backend.tests.unit.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import unq.pdes.backend.search.TravelPackageSearchPageable

class TravelPackageSearchPageableTest {

    @Test
    fun `sanitize should default to sorting by name ascending when unsorted`() {
        val sanitized = TravelPackageSearchPageable.sanitize(PageRequest.of(0, 20))

        assertEquals(Sort.by(Sort.Order.asc("name")), sanitized.sort)
    }

    @Test
    fun `sanitize should keep a whitelisted sort`() {
        val sanitized = TravelPackageSearchPageable.sanitize(PageRequest.of(0, 20, Sort.by(Sort.Order.desc("price"))))

        assertEquals(Sort.by(Sort.Order.desc("price")), sanitized.sort)
    }

    @Test
    fun `sanitize should cap the page size`() {
        val sanitized = TravelPackageSearchPageable.sanitize(PageRequest.of(0, 500))

        assertEquals(50, sanitized.pageSize)
    }

    @Test
    fun `sanitize should reject sorting by a non whitelisted property`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            TravelPackageSearchPageable.sanitize(PageRequest.of(0, 20, Sort.by("agency.id")))
        }

        assertEquals("Cannot sort packages by 'agency.id'.", exception.message)
    }
}
