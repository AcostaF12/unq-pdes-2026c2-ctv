package unq.pdes.backend.service

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.distribution.CountAtBucket
import org.springframework.boot.health.contributor.CompositeHealthContributor
import org.springframework.boot.health.contributor.HealthContributor
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.health.registry.HealthContributorRegistry
import org.springframework.stereotype.Service
import unq.pdes.backend.system.CircuitBreakerSnapshot
import unq.pdes.backend.system.ConcurrencySnapshot
import unq.pdes.backend.system.HttpEndpointSample
import unq.pdes.backend.system.HttpTraffic
import unq.pdes.backend.system.SystemComponent
import unq.pdes.backend.system.SystemMeters
import unq.pdes.backend.system.SystemSnapshot
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Service
class SystemSnapshotService(
    private val meterRegistry: MeterRegistry,
    private val healthContributors: HealthContributorRegistry,
    private val circuitBreakers: CircuitBreakerRegistry,
) {
    fun snapshot(): SystemSnapshot =
        SystemSnapshot(
            capturedAt = Instant.now(),
            systems =
                listOf(
                    SystemComponent(SystemMeters.BACKEND, SystemMeters.UP),
                    SystemComponent(SystemMeters.DATABASE, statusOf(SystemMeters.DATABASE_HEALTH)),
                    SystemComponent(SystemMeters.FLIGHTS, statusOf(SystemMeters.FLIGHTS_HEALTH)),
                ),
            http = httpTraffic(),
            concurrency = concurrency(),
            circuitBreaker = circuitBreaker(),
        )

    private fun httpTraffic(): HttpTraffic {
        val counted =
            meterRegistry
                .find(SystemMeters.HTTP_SERVER_REQUESTS)
                .timers()
                .mapNotNull { it.toCounted() }
                .filter { !it.uri.startsWith(SystemMeters.ACTUATOR_PREFIX) }
        val requestCount = counted.sumOf { it.count }
        val totalMillis = counted.sumOf { it.totalMillis }
        return HttpTraffic(
            requestCount = requestCount,
            clientErrors = counted.filter { it.status.startsWith("4") }.sumOf { it.count },
            serverErrors = counted.filter { it.status.startsWith("5") }.sumOf { it.count },
            meanMillis = if (requestCount == 0L) null else totalMillis / requestCount,
            endpoints = topEndpoints(counted),
        )
    }

    private fun topEndpoints(series: List<CountedRequest>): List<HttpEndpointSample> =
        series
            .groupBy { it.method to it.uri }
            .map { (key, rows) ->
                val dominant = rows.maxWith(compareBy<CountedRequest> { it.count }.thenBy { successRank(it.status) })
                RankedEndpoint(
                    method = key.first,
                    uri = key.second,
                    requestCount = rows.sumOf { it.count },
                    serverErrors = rows.filter { it.status.startsWith("5") }.sumOf { it.count },
                    dominant = dominant.timer,
                )
            }.sortedWith(
                compareByDescending<RankedEndpoint> { it.requestCount }.thenBy { it.method }.thenBy { it.uri },
            ).take(SystemMeters.TOP_ENDPOINTS)
            .map { ranked ->
                HttpEndpointSample(
                    method = ranked.method,
                    uri = ranked.uri,
                    requestCount = ranked.requestCount,
                    serverErrors = ranked.serverErrors,
                    p95Millis = ranked.dominant.p95Millis(),
                )
            }

    private fun Timer.toCounted(): CountedRequest? {
        val uri = id.getTag("uri") ?: return null
        return CountedRequest(
            timer = this,
            method = id.getTag("method") ?: "UNKNOWN",
            uri = uri,
            status = id.getTag("status") ?: "",
            count = count(),
            totalMillis = totalTime(TimeUnit.MILLISECONDS),
        )
    }

    private fun Timer.p95Millis(): Double? {
        val snapshot = takeSnapshot()
        val fromHistogram = histogramPercentileMillis(snapshot.histogramCounts(), count().toDouble())
        if (fromHistogram != null) {
            return fromHistogram
        }
        val value =
            snapshot
                .percentileValues()
                .firstOrNull { abs(it.percentile() - SystemMeters.P95) < SystemMeters.PERCENTILE_TOLERANCE }
                ?.value(TimeUnit.MILLISECONDS)
                ?: return null
        return value.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun histogramPercentileMillis(
        buckets: Array<CountAtBucket>,
        total: Double,
    ): Double? {
        if (buckets.isEmpty() || total <= 0.0) {
            return null
        }
        val target = SystemMeters.P95 * total
        val bucket = buckets.firstOrNull { it.count() >= target } ?: buckets.last()
        val millis = bucket.bucket(TimeUnit.MILLISECONDS)
        return millis.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun concurrency(): ConcurrencySnapshot =
        ConcurrencySnapshot(
            poolActive = gaugeSum(SystemMeters.POOL_ACTIVE),
            poolIdle = gaugeSum(SystemMeters.POOL_IDLE),
            poolPending = gaugeSum(SystemMeters.POOL_PENDING),
            poolMax = gaugeSum(SystemMeters.POOL_MAX),
            threadsBusy = gaugeSum(SystemMeters.THREADS_BUSY),
            threadsCurrent = gaugeSum(SystemMeters.THREADS_CURRENT),
            threadsMax = gaugeSum(SystemMeters.THREADS_MAX),
        )

    private fun gaugeSum(name: String): Double? {
        val values =
            meterRegistry
                .find(name)
                .gauges()
                .map { it.value() }
                .filter { it.isFinite() }
        if (values.isEmpty()) {
            return null
        }
        return values.sum()
    }

    private fun circuitBreaker(): CircuitBreakerSnapshot {
        val breaker = circuitBreakers.find(SystemMeters.FLIGHTS_BREAKER).orElse(null)
        return CircuitBreakerSnapshot(
            name = SystemMeters.FLIGHTS_BREAKER,
            state = breaker?.state?.name ?: SystemMeters.UNKNOWN,
            failureRate = breaker?.let { failureRate(it) },
        )
    }

    private fun failureRate(breaker: CircuitBreaker): Double? {
        val rate = breaker.metrics.failureRate
        return if (rate < 0f) null else rate.toDouble()
    }

    private fun statusOf(name: String): String {
        val contributor = healthContributors.getContributor(name) ?: return SystemMeters.UNKNOWN
        return statusOf(contributor)
    }

    private fun statusOf(contributor: HealthContributor): String =
        try {
            if (contributor is HealthIndicator) {
                contributor.health()?.status?.code ?: SystemMeters.UNKNOWN
            } else if (contributor is CompositeHealthContributor) {
                aggregate(contributor)
            } else {
                SystemMeters.UNKNOWN
            }
        } catch (_: Exception) {
            SystemMeters.DOWN
        }

    private fun aggregate(composite: CompositeHealthContributor): String {
        val statuses = composite.stream().map { statusOf(it.contributor()) }.toList()
        return when {
            statuses.isEmpty() -> SystemMeters.UNKNOWN
            statuses.any { it == SystemMeters.DOWN } -> SystemMeters.DOWN
            statuses.any { it == SystemMeters.OUT_OF_SERVICE } -> SystemMeters.OUT_OF_SERVICE
            statuses.any { it == SystemMeters.UNKNOWN } -> SystemMeters.UNKNOWN
            statuses.all { it == SystemMeters.UP } -> SystemMeters.UP
            else -> SystemMeters.UNKNOWN
        }
    }

    private fun successRank(status: String): Int = if (status.startsWith("2")) 1 else 0
}

private data class CountedRequest(
    val timer: Timer,
    val method: String,
    val uri: String,
    val status: String,
    val count: Long,
    val totalMillis: Double,
)

private data class RankedEndpoint(
    val method: String,
    val uri: String,
    val requestCount: Long,
    val serverErrors: Long,
    val dominant: Timer,
)
