package unq.pdes.backend.tests.unit.external

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.boot.health.contributor.Status
import org.springframework.web.client.RestClient
import unq.pdes.backend.external.flights.FlightsServiceHealthIndicator
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@TestInstance(PER_CLASS)
class FlightsServiceHealthIndicatorTest {
    @Test
    fun `01 - reports UP when the flights service responds with UP status`() {
        val server = healthServer(200, """{"status":"UP"}""")
        try {
            val health = indicator(server).health()

            assertEquals(Status.UP, health.status)
            assertEquals(url(server), health.details["url"])
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `02 - reports DOWN when the flights service responds with a non-UP status`() {
        val server = healthServer(200, """{"status":"DOWN"}""")
        try {
            val health = indicator(server).health()

            assertEquals(Status.DOWN, health.status)
            assertEquals(url(server), health.details["url"])
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `03 - reports DOWN when the flights service call fails`() {
        val port = ServerSocket(0).use { it.localPort }
        val health = FlightsServiceHealthIndicator("http://127.0.0.1:$port", RestClient.builder()).health()

        assertEquals(Status.DOWN, health.status)
        assertEquals("http://127.0.0.1:$port", health.details["url"])
    }

    @Test
    fun `05 - returns the last check without waiting when the next probe is slow`() {
        val calls = AtomicInteger()
        val server =
            healthServer(200, """{"status":"UP"}""", calls) { call ->
                if (call > 1) {
                    Thread.sleep(2_000)
                }
            }
        val indicator = FlightsServiceHealthIndicator.create(url(server), RestClient.builder(), cacheTtlNanos = 0)
        try {
            assertEquals(Status.UP, indicator.health().status)

            val elapsed = measureTimeMillis { assertEquals(Status.UP, indicator.health().status) }

            assertTrue(elapsed < 100) { "stale health took ${elapsed}ms" }
        } finally {
            indicator.close()
            server.stop(0)
        }
    }

    @Test
    fun `04 - reuses the last check when the screen and the probe ask together`() {
        val calls = AtomicInteger()
        val server = healthServer(200, """{"status":"UP"}""", calls)
        try {
            val indicator = indicator(server)

            val first = indicator.health()
            val second = indicator.health()

            assertEquals(Status.UP, first.status)
            assertEquals(Status.UP, second.status)
            assertEquals(1, calls.get())
        } finally {
            server.stop(0)
        }
    }

    private fun indicator(server: HttpServer): FlightsServiceHealthIndicator =
        FlightsServiceHealthIndicator(url(server), RestClient.builder())

    private fun url(server: HttpServer): String = "http://127.0.0.1:${server.address.port}"

    private fun healthServer(
        status: Int,
        body: String,
        calls: AtomicInteger = AtomicInteger(),
        onCall: (Int) -> Unit = {},
    ): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/actuator/health") { exchange ->
            val call = calls.incrementAndGet()
            onCall(call)
            val bytes = body.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        return server
    }
}
