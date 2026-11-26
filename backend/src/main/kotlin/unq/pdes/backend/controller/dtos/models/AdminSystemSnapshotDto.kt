package unq.pdes.backend.controller.dtos.models

import java.time.Instant

data class AdminSystemSnapshotDto(
    val capturedAt: Instant,
    val systems: List<AdminSystemComponentDto>,
    val http: AdminHttpTrafficDto,
    val concurrency: AdminConcurrencyDto,
    val circuitBreaker: AdminCircuitBreakerDto,
)

data class AdminSystemComponentDto(
    val name: String,
    val status: String,
)

data class AdminHttpTrafficDto(
    val requestCount: Long,
    val clientErrors: Long,
    val serverErrors: Long,
    val meanMillis: Double?,
    val endpoints: List<AdminHttpEndpointDto>,
)

data class AdminHttpEndpointDto(
    val method: String,
    val uri: String,
    val requestCount: Long,
    val serverErrors: Long,
    val p95Millis: Double?,
)

data class AdminConcurrencyDto(
    val poolActive: Double?,
    val poolIdle: Double?,
    val poolPending: Double?,
    val poolMax: Double?,
    val threadsBusy: Double?,
    val threadsCurrent: Double?,
    val threadsMax: Double?,
)

data class AdminCircuitBreakerDto(
    val name: String,
    val state: String,
    val failureRate: Double?,
)
