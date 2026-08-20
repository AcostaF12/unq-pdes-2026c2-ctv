package unq.pdes.backend.config

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ActuatorTest {

	@Value("\${local.server.port}")
	private var port: Int = 0

	private val client = HttpClient.newHttpClient()

	private fun get(path: String): HttpResponse<String> =
		client.send(
			HttpRequest.newBuilder(URI.create("http://localhost:$port$path")).GET().build(),
			HttpResponse.BodyHandlers.ofString(),
		)

	@Test
	fun `health endpoint should report the service is up`() {
		val response = get("/actuator/health")

		assertEquals(200, response.statusCode())
		assertContains(response.body(), "\"status\":\"UP\"")
	}
}
