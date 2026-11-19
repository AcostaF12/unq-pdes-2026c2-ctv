package unq.pdes.backend.config.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Logs one INFO line per HTTP request (method, path, status and duration).
 *
 * Ordered right after Spring Boot's ServerHttpObservationFilter (HIGHEST_PRECEDENCE + 1), so the
 * observation scope is already open and the MDC carries the request's traceId/spanId, and before
 * Spring Security's filter chain (order -100), so requests rejected there (401/403) are logged too.
 * Only the method, path and status are logged: never headers, tokens or bodies.
 * Actuator endpoints are skipped to keep health checks and Prometheus scrapes out of the index.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val requestLogger = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI.startsWith("/actuator")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val start = System.nanoTime()
        var status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        try {
            filterChain.doFilter(request, response)
            status = response.status
        } finally {
            val durationMs = (System.nanoTime() - start) / 1_000_000
            requestLogger.info("{} {} -> {} ({} ms)", request.method, request.requestURI, status, durationMs)
        }
    }
}
