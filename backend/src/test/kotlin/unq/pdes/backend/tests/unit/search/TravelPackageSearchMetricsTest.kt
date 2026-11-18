package unq.pdes.backend.tests.unit.search

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import unq.pdes.backend.search.TravelPackageSearchCriteria
import unq.pdes.backend.search.TravelPackageSearchMetrics
import java.time.Duration

class TravelPackageSearchMetricsTest {
    private val registry = SimpleMeterRegistry()
    private val metrics = TravelPackageSearchMetrics(registry)

    @Test
    fun `record should tag the latency timer with the filters actually used`() {
        val criteria = TravelPackageSearchCriteria.from("Paris", null, "PAR", null, null)

        metrics.record(criteria, resultCount = 3, duration = Duration.ofMillis(120))

        val timer =
            registry
                .get("package_search_duration")
                .tag("filter_name", "true")
                .tag("filter_origin", "false")
                .tag("filter_destination", "true")
                .tag("filter_price", "false")
                .timer()

        assertEquals(1, timer.count())
    }

    @Test
    fun `record should track the number of results returned`() {
        val criteria = TravelPackageSearchCriteria.from(null, null, null, null, null)

        metrics.record(criteria, resultCount = 7, duration = Duration.ofMillis(10))

        val summary = registry.get("package_search_results").summary()

        assertEquals(1, summary.count())
        assertEquals(7.0, summary.max())
    }
}
