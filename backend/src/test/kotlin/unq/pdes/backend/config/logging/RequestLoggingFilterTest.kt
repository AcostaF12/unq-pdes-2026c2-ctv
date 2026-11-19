package unq.pdes.backend.config.logging

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension::class)
class RequestLoggingFilterTest {
    @Value("\${local.server.port}")
    private var port: Int = 0

    private val client = HttpClient.newHttpClient()

    private fun get(path: String): HttpResponse<String> =
        client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:$port$path")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    @Test
    fun `logs method, path, status and duration of each request`(output: CapturedOutput) {
        val response = get("/v3/api-docs")

        assertEquals(200, response.statusCode())
        assertContains(output.out, Regex("""GET /v3/api-docs -> 200 \(\d+ ms\)"""))
    }

    @Test
    fun `logs requests rejected by security with their status`(output: CapturedOutput) {
        val response = get("/packages/agency")

        assertEquals(403, response.statusCode())
        assertContains(output.out, Regex("""GET /packages/agency -> 403 \(\d+ ms\)"""))
    }

    @Test
    fun `does not log actuator requests`(output: CapturedOutput) {
        val response = get("/actuator/health")

        assertEquals(200, response.statusCode())
        assertFalse(output.out.contains("/actuator/health"))
    }
}
