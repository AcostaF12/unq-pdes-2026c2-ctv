package unq.pdes.backend.external.flights

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Component
@Profile("!test")
class FlightsServiceHealthIndicator private constructor(
    private val flightsServiceUrl: String,
    restClientBuilder: RestClient.Builder,
    private val cacheTtlNanos: Long,
) : HealthIndicator {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var cached: CachedHealth? = null

    private val refreshScheduled = AtomicBoolean(false)

    private val refresher: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "flights-health").apply { isDaemon = true }
        }

    private val restClient =
        restClientBuilder
            .clone()
            .requestFactory(healthRequestFactory())
            .build()

    @Autowired
    constructor(
        @Value("\${flights.service.url}") flightsServiceUrl: String,
        restClientBuilder: RestClient.Builder,
    ) : this(flightsServiceUrl, restClientBuilder, CACHE_TTL_NANOS)

    override fun health(): Health {
        val current = cached
        if (current == null) {
            synchronized(this) {
                val again = cached
                if (again != null) {
                    return freshOrStale(again)
                }
                return probeAndStore()
            }
        }
        return freshOrStale(current)
    }

    @PreDestroy
    fun close() {
        refresher.shutdownNow()
    }

    private fun freshOrStale(current: CachedHealth): Health {
        if (!current.isExpired(cacheTtlNanos)) {
            return current.health
        }
        scheduleRefresh()
        return current.health
    }

    private fun scheduleRefresh() {
        if (!refreshScheduled.compareAndSet(false, true)) {
            return
        }
        refresher.execute {
            try {
                probeAndStore()
            } finally {
                refreshScheduled.set(false)
            }
        }
    }

    private fun probeAndStore(): Health {
        val fresh = probe()
        cached = CachedHealth(fresh, System.nanoTime())
        return fresh
    }

    private fun probe(): Health {
        logger.debug("Checking external flights service health at {}", flightsServiceUrl)

        return try {
            val body =
                restClient
                    .get()
                    .uri("$flightsServiceUrl/actuator/health")
                    .retrieve()
                    .body<String>()

            if (body != null && body.contains("\"status\":\"UP\"")) {
                logger.debug("Flights service health check succeeded for {}", flightsServiceUrl)
                Health.up().withDetail("url", flightsServiceUrl).build()
            } else {
                logger.warn("Flights service health check returned non-UP status for {}", flightsServiceUrl)
                Health.down().withDetail("url", flightsServiceUrl).build()
            }
        } catch (ex: Exception) {
            logger.error("Flights service health check failed for {}", flightsServiceUrl, ex)
            Health.down(ex).withDetail("url", flightsServiceUrl).build()
        }
    }

    private fun healthRequestFactory(): SimpleClientHttpRequestFactory =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(HEALTH_TIMEOUT)
            setReadTimeout(HEALTH_TIMEOUT)
        }

    private class CachedHealth(
        val health: Health,
        val atNanos: Long,
    ) {
        fun isExpired(ttlNanos: Long): Boolean = System.nanoTime() - atNanos >= ttlNanos
    }

    companion object {
        val HEALTH_TIMEOUT: Duration = Duration.ofMillis(800)
        val CACHE_TTL_NANOS: Long = Duration.ofSeconds(2).toNanos()

        internal fun create(
            flightsServiceUrl: String,
            restClientBuilder: RestClient.Builder,
            cacheTtlNanos: Long,
        ): FlightsServiceHealthIndicator = FlightsServiceHealthIndicator(flightsServiceUrl, restClientBuilder, cacheTtlNanos)
    }
}
