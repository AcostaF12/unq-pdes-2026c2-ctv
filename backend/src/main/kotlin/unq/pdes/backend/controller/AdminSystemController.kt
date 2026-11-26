package unq.pdes.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.dtos.models.AdminCircuitBreakerDto
import unq.pdes.backend.controller.dtos.models.AdminConcurrencyDto
import unq.pdes.backend.controller.dtos.models.AdminHttpEndpointDto
import unq.pdes.backend.controller.dtos.models.AdminHttpTrafficDto
import unq.pdes.backend.controller.dtos.models.AdminSystemComponentDto
import unq.pdes.backend.controller.dtos.models.AdminSystemSnapshotDto
import unq.pdes.backend.service.SystemSnapshotService
import unq.pdes.backend.system.CircuitBreakerSnapshot
import unq.pdes.backend.system.ConcurrencySnapshot
import unq.pdes.backend.system.HttpEndpointSample
import unq.pdes.backend.system.HttpTraffic
import unq.pdes.backend.system.SystemComponent
import unq.pdes.backend.system.SystemSnapshot

@Tag(
    name = "Administrative system",
    description = "Instantánea de salud, tráfico HTTP, pool, hilos y circuit breaker. Solo admin. No scrapea Prometheus.",
)
@RestController
class AdminSystemController(
    private val service: SystemSnapshotService,
) {
    @Operation(
        summary = "Leer el estado operativo del backend",
        description =
            "Devuelve el instante actual del proceso: salud de la base y de flights-service, " +
                "requests HTTP acumulados desde el arranque (sin /actuator), pool de Hikari, " +
                "hilos de Tomcat y el circuit breaker flightsService.\n\n" +
                "El p95 es el de la serie con más requests de cada endpoint, no el cuantil global.\n\n" +
                "**Quién:** admin",
    )
    @GetMapping("/admin/system")
    fun system(): AdminSystemSnapshotDto = service.snapshot().toDto()

    private fun SystemSnapshot.toDto(): AdminSystemSnapshotDto =
        AdminSystemSnapshotDto(
            capturedAt = capturedAt,
            systems = systems.map { it.toDto() },
            http = http.toDto(),
            concurrency = concurrency.toDto(),
            circuitBreaker = circuitBreaker.toDto(),
        )

    private fun SystemComponent.toDto(): AdminSystemComponentDto = AdminSystemComponentDto(name = name, status = status)

    private fun HttpTraffic.toDto(): AdminHttpTrafficDto =
        AdminHttpTrafficDto(
            requestCount = requestCount,
            clientErrors = clientErrors,
            serverErrors = serverErrors,
            meanMillis = meanMillis,
            endpoints = endpoints.map { it.toDto() },
        )

    private fun HttpEndpointSample.toDto(): AdminHttpEndpointDto =
        AdminHttpEndpointDto(
            method = method,
            uri = uri,
            requestCount = requestCount,
            serverErrors = serverErrors,
            p95Millis = p95Millis,
        )

    private fun ConcurrencySnapshot.toDto(): AdminConcurrencyDto =
        AdminConcurrencyDto(
            poolActive = poolActive,
            poolIdle = poolIdle,
            poolPending = poolPending,
            poolMax = poolMax,
            threadsBusy = threadsBusy,
            threadsCurrent = threadsCurrent,
            threadsMax = threadsMax,
        )

    private fun CircuitBreakerSnapshot.toDto(): AdminCircuitBreakerDto =
        AdminCircuitBreakerDto(name = name, state = state, failureRate = failureRate)
}
