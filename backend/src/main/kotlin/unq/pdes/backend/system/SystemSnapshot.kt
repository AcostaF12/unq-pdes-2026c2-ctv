package unq.pdes.backend.system

import java.time.Instant

data class SystemSnapshot(
    val capturedAt: Instant,
    val systems: List<SystemComponent>,
    val http: HttpTraffic,
    val concurrency: ConcurrencySnapshot,
    val circuitBreaker: CircuitBreakerSnapshot,
)

data class SystemComponent(
    val name: String,
    val status: String,
)

data class HttpTraffic(
    val requestCount: Long,
    val clientErrors: Long,
    val serverErrors: Long,
    val meanMillis: Double?,
    val endpoints: List<HttpEndpointSample>,
)

data class HttpEndpointSample(
    val method: String,
    val uri: String,
    val requestCount: Long,
    val serverErrors: Long,
    val p95Millis: Double?,
)

data class ConcurrencySnapshot(
    val poolActive: Double?,
    val poolIdle: Double?,
    val poolPending: Double?,
    val poolMax: Double?,
    val threadsBusy: Double?,
    val threadsCurrent: Double?,
    val threadsMax: Double?,
)

data class CircuitBreakerSnapshot(
    val name: String,
    val state: String,
    val failureRate: Double?,
)

internal object SystemMeters {
    const val BACKEND = "backend"
    const val DATABASE = "database"
    const val FLIGHTS = "flights"
    const val DATABASE_HEALTH = "db"
    const val FLIGHTS_HEALTH = "flightsService"
    const val FLIGHTS_BREAKER = "flightsService"
    const val HTTP_SERVER_REQUESTS = "http.server.requests"
    const val ACTUATOR_PREFIX = "/actuator"
    const val TOP_ENDPOINTS = 5
    const val P95 = 0.95
    const val PERCENTILE_TOLERANCE = 0.001
    const val UP = "UP"
    const val DOWN = "DOWN"
    const val UNKNOWN = "UNKNOWN"
    const val OUT_OF_SERVICE = "OUT_OF_SERVICE"
    const val POOL_ACTIVE = "hikaricp.connections.active"
    const val POOL_IDLE = "hikaricp.connections.idle"
    const val POOL_PENDING = "hikaricp.connections.pending"
    const val POOL_MAX = "hikaricp.connections.max"
    const val THREADS_BUSY = "tomcat.threads.busy"
    const val THREADS_CURRENT = "tomcat.threads.current"
    const val THREADS_MAX = "tomcat.threads.config.max"
}
