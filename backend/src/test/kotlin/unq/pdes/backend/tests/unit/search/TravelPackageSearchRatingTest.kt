package unq.pdes.backend.tests.unit.search

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import unq.pdes.backend.search.TravelPackageSearchCriteria
import unq.pdes.backend.search.TravelPackageSearchMetrics
import unq.pdes.backend.search.TravelPackageSearchPageable
import java.math.BigDecimal
import java.time.Duration

class TravelPackageSearchRatingTest {
    @Test
    fun `criteria should normalize the new filters`() {
        val criteria = TravelPackageSearchCriteria.from(null, null, null, null, null, 7L, "  Hotel Sol ", BigDecimal("8.5"))

        assertEquals(7L, criteria.agencyId)
        assertEquals("Hotel Sol", criteria.hotel)
        assertEquals(BigDecimal("8.5"), criteria.minRating)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun `criteria with only blank new filters should be empty`() {
        val criteria = TravelPackageSearchCriteria.from(null, null, null, null, null, null, "  ", null)

        assertNull(criteria.hotel)
        assertEquals(true, criteria.isEmpty())
    }

    @Test
    fun `sanitize should accept sorting by rating and destination name`() {
        val rating = TravelPackageSearchPageable.sanitize(PageRequest.of(0, 20, Sort.by(Sort.Order.desc("rating"))))
        val destination = TravelPackageSearchPageable.sanitize(PageRequest.of(0, 20, Sort.by("destination.name")))

        assertEquals(Sort.by(Sort.Order.desc("rating")), rating.sort)
        assertEquals(Sort.by("destination.name"), destination.sort)
    }

    @Test
    fun `ratingDirection should only exist when sorting by rating`() {
        val byRating = PageRequest.of(0, 20, Sort.by(Sort.Order.asc("rating")))
        val byPrice = PageRequest.of(0, 20, Sort.by("price"))

        assertEquals(Sort.Direction.ASC, TravelPackageSearchPageable.ratingDirection(byRating))
        assertNull(TravelPackageSearchPageable.ratingDirection(byPrice))
    }

    @Test
    fun `withoutRating should strip the computed order and keep the paging`() {
        val byRating = PageRequest.of(2, 10, Sort.by(Sort.Order.desc("rating")))
        val byPrice = PageRequest.of(2, 10, Sort.by("price"))

        val stripped = TravelPackageSearchPageable.withoutRating(byRating)

        assertEquals(Sort.unsorted(), stripped.sort)
        assertEquals(2, stripped.pageNumber)
        assertEquals(10, stripped.pageSize)
        assertEquals(byPrice, TravelPackageSearchPageable.withoutRating(byPrice))
    }

    @Test
    fun `metrics should tag the agency hotel and rating filters`() {
        val registry = SimpleMeterRegistry()
        val criteria = TravelPackageSearchCriteria.from(null, null, null, null, null, 3L, "Sol", BigDecimal("5"))

        TravelPackageSearchMetrics(registry).record(criteria, 1, Duration.ofMillis(5))

        val timer =
            registry
                .get("package_search_duration")
                .tag("filter_agency", "true")
                .tag("filter_hotel", "true")
                .tag("filter_rating", "true")
                .timer()
        assertEquals(1, timer.count())
    }
}
