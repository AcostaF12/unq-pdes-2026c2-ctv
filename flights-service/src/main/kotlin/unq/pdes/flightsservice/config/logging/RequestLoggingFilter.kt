package unq.pdes.flightsservice.config.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Logs one INFO line per HTTP request (method, path, status and duration).
 *
 * Registered with the default (lowest) filter order, so it runs inside Spring Boot's
 * ServerHttpObservationFilter: the observation scope is open and the MDC already carries
 * the request's traceId/spanId, which lets Kibana correlate these lines with the caller.
 * Actuator endpoints are skipped to keep health checks and Prometheus scrapes out of the index.
 */
@Component
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
