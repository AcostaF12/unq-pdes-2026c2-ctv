package unq.pdes.backend.search

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TravelPackageSearchMetrics(
    private val meterRegistry: MeterRegistry,
) {
    fun record(
        criteria: TravelPackageSearchCriteria,
        resultCount: Long,
        duration: Duration,
    ) {
        Timer
            .builder("package_search_duration")
            .description("Latency of travel package search queries")
            .tag("filter_name", criteria.name.isUsed())
            .tag("filter_origin", criteria.origin.isUsed())
            .tag("filter_destination", criteria.destination.isUsed())
            .tag("filter_price", (criteria.minPrice != null || criteria.maxPrice != null).toTag())
            .register(meterRegistry)
            .record(duration)

        DistributionSummary
            .builder("package_search_results")
            .description("Number of results returned by travel package search queries")
            .register(meterRegistry)
            .record(resultCount.toDouble())
    }

    private fun Any?.isUsed(): String = (this != null).toTag()

    private fun Boolean.toTag(): String = if (this) "true" else "false"
}
