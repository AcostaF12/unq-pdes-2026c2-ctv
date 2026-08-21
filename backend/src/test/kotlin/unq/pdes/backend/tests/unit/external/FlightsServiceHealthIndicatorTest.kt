package unq.pdes.backend.tests.unit.external

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.boot.health.contributor.Status
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import unq.pdes.backend.external.flights.FlightsServiceHealthIndicator

@TestInstance(PER_CLASS)
class FlightsServiceHealthIndicatorTest {

    private val flightsServiceUrl = "http://flights-service:8081"

    private lateinit var server: MockRestServiceServer
    private lateinit var healthIndicator: FlightsServiceHealthIndicator

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        healthIndicator = FlightsServiceHealthIndicator(flightsServiceUrl, builder)
    }

    @Test
    fun `01 - reports UP when the flights service responds with UP status`() {
        server.expect(requestTo("$flightsServiceUrl/actuator/health"))
            .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON))

        val health = healthIndicator.health()

        assertEquals(Status.UP, health.status)
        assertEquals(flightsServiceUrl, health.details["url"])
    }

    @Test
    fun `02 - reports DOWN when the flights service responds with a non-UP status`() {
        server.expect(requestTo("$flightsServiceUrl/actuator/health"))
            .andRespond(withSuccess("{\"status\":\"DOWN\"}", MediaType.APPLICATION_JSON))

        val health = healthIndicator.health()

        assertEquals(Status.DOWN, health.status)
        assertEquals(flightsServiceUrl, health.details["url"])
    }

    @Test
    fun `03 - reports DOWN when the flights service call fails`() {
        server.expect(requestTo("$flightsServiceUrl/actuator/health"))
            .andRespond(withServerError())

        val health = healthIndicator.health()

        assertEquals(Status.DOWN, health.status)
        assertEquals(flightsServiceUrl, health.details["url"])
    }
}
