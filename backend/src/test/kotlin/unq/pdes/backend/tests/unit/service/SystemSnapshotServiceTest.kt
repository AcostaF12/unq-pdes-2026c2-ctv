package unq.pdes.backend.tests.unit.service

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.health.contributor.CompositeHealthContributor
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry
import unq.pdes.backend.service.SystemSnapshotService
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SystemSnapshotServiceTest {
    private val meters = SimpleMeterRegistry()
    private val health = DefaultHealthContributorRegistry()
    private val breakers = CircuitBreakerRegistry.ofDefaults()
    private val poolActive = AtomicInteger(4)
    private val poolIdle = AtomicInteger(6)
    private val poolPending = AtomicInteger(1)
    private val poolMax = AtomicInteger(10)

    @Test
    fun `snapshot sums http traffic, skips actuator and keeps the five busiest endpoints`() {
        record("GET", "/packages", "200", millis = 100, times = 10)
        record("GET", "/packages", "500", millis = 400, times = 2)
        record("POST", "/auth/login", "200", millis = 20, times = 8)
        record("GET", "/hotels", "200", millis = 30, times = 6)
        record("GET", "/flights", "200", millis = 40, times = 4)
        record("POST", "/purchases", "400", millis = 15, times = 3)
        record("GET", "/cities", "200", millis = 5, times = 1)
        record("GET", "/actuator/prometheus", "200", millis = 1, times = 50)
        health.registerContributor("db", HealthIndicator { Health.up().build() })
        health.registerContributor("flightsService", HealthIndicator { Health.down().build() })
        registerPool()
        breakers.circuitBreaker("flightsService").transitionToOpenState()

        val snapshot = service().snapshot()

        assertEquals("UP", snapshot.systems.first { it.name == "backend" }.status)
        assertEquals("UP", snapshot.systems.first { it.name == "database" }.status)
        assertEquals("DOWN", snapshot.systems.first { it.name == "flights" }.status)
        assertEquals(34, snapshot.http.requestCount)
        assertEquals(3, snapshot.http.clientErrors)
        assertEquals(2, snapshot.http.serverErrors)
        assertEquals(5, snapshot.http.endpoints.size)
        val packages = snapshot.http.endpoints.first()
        assertEquals("GET", packages.method)
        assertEquals("/packages", packages.uri)
        assertEquals(12, packages.requestCount)
        assertEquals(2, packages.serverErrors)
        val p95 = packages.p95Millis
        assertTrue(p95 != null && p95 in 50.0..250.0) { "p95=$p95" }
        assertEquals(2350.0 / 34, snapshot.http.meanMillis!!, 1.0)
        assertTrue(snapshot.http.endpoints.none { it.uri == "/cities" || it.uri.startsWith("/actuator") })
        assertEquals(4.0, snapshot.concurrency.poolActive)
        assertEquals(6.0, snapshot.concurrency.poolIdle)
        assertEquals(1.0, snapshot.concurrency.poolPending)
        assertEquals(10.0, snapshot.concurrency.poolMax)
        assertNull(snapshot.concurrency.threadsBusy)
        assertEquals("flightsService", snapshot.circuitBreaker.name)
        assertEquals("OPEN", snapshot.circuitBreaker.state)
        assertNull(snapshot.circuitBreaker.failureRate)
    }

    @Test
    fun `snapshot reports unknown pieces and a down database composite when meters are missing`() {
        health.registerContributor(
            "db",
            CompositeHealthContributor.fromMap(mapOf("h2" to HealthIndicator { Health.down().build() })),
        )

        val snapshot = service().snapshot()

        assertEquals("DOWN", snapshot.systems.first { it.name == "database" }.status)
        assertEquals("UNKNOWN", snapshot.systems.first { it.name == "flights" }.status)
        assertEquals(0, snapshot.http.requestCount)
        assertNull(snapshot.http.meanMillis)
        assertTrue(snapshot.http.endpoints.isEmpty())
        assertNull(snapshot.concurrency.poolActive)
        assertNull(snapshot.concurrency.poolIdle)
        assertNull(snapshot.concurrency.poolPending)
        assertNull(snapshot.concurrency.poolMax)
        assertNull(snapshot.concurrency.threadsCurrent)
        assertNull(snapshot.concurrency.threadsMax)
        assertEquals("UNKNOWN", snapshot.circuitBreaker.state)
        assertNull(snapshot.circuitBreaker.failureRate)
    }

    @Test
    fun `snapshot reads the circuit breaker failure rate once there are enough calls`() {
        val config =
            CircuitBreakerConfig
                .custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .build()
        val registry = CircuitBreakerRegistry.of(config)
        val breaker = registry.circuitBreaker("flightsService")
        breaker.onError(0, TimeUnit.NANOSECONDS, IllegalStateException("down"))
        breaker.onError(0, TimeUnit.NANOSECONDS, IllegalStateException("down"))
        val service = SystemSnapshotService(meters, health, registry)

        val snapshot = service.snapshot()

        assertEquals(100.0, snapshot.circuitBreaker.failureRate)
    }

    private fun service(): SystemSnapshotService = SystemSnapshotService(meters, health, breakers)

    private fun registerPool() {
        Gauge.builder("hikaricp.connections.active", poolActive) { it.get().toDouble() }.register(meters)
        Gauge.builder("hikaricp.connections.idle", poolIdle) { it.get().toDouble() }.register(meters)
        Gauge.builder("hikaricp.connections.pending", poolPending) { it.get().toDouble() }.register(meters)
        Gauge.builder("hikaricp.connections.max", poolMax) { it.get().toDouble() }.register(meters)
    }

    private fun record(
        method: String,
        uri: String,
        status: String,
        millis: Long,
        times: Int,
    ) {
        val timer =
            Timer
                .builder("http.server.requests")
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", status)
                .tag("outcome", "SUCCESS")
                .tag("exception", "none")
                .publishPercentiles(0.95)
                .publishPercentileHistogram()
                .register(meters)
        repeat(times) { timer.record(Duration.ofMillis(millis)) }
    }
}
