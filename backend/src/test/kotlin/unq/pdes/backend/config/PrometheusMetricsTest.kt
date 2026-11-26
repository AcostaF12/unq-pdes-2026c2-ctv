package unq.pdes.backend.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertContains
import kotlin.test.assertEquals

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "resilience4j.circuitbreaker.instances.flightsService.sliding-window-size=10",
        // Tests disable metrics export. This turns the Prometheus scrape back on for this case only.
        "management.prometheus.metrics.export.enabled=true",
    ],
)
@ActiveProfiles("test")
class PrometheusMetricsTest {
    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client = HttpClient.newHttpClient()

    private fun get(path: String): HttpResponse<String> =
        client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:$port$path")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    @Test
    fun `prometheus should expose latency buckets after a request`() {
        val health = get("/actuator/health")
        assertEquals(200, health.statusCode())

        val response = get("/actuator/prometheus")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "http_server_requests_seconds_bucket")
        assertContains(response.body(), "hikaricp_connections_active")
        assertContains(response.body(), "hikaricp_connections_pending")
        assertContains(response.body(), "resilience4j_circuitbreaker_state")
    }
}
