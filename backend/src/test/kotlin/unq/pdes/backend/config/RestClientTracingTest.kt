package unq.pdes.backend.config

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.tracing.Tracer
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import unq.pdes.backend.external.flights.FlightsClient

/**
 * FlightsClient must use the RestClient.Builder auto-configured by Spring Boot, so outgoing
 * calls create CLIENT spans and propagate the W3C trace context to flights-service.
 * A hand-made RestClient.builder() bean has no ObservationRegistry and breaks the trace.
 */
@SpringBootTest(
    properties = [
        "management.tracing.export.enabled=true",
        "management.tracing.export.zipkin.enabled=false",
    ],
)
@AutoConfigureTracing
@ActiveProfiles("test")
class RestClientTracingTest {
    @Autowired
    private lateinit var restClientBuilder: RestClient.Builder

    @Autowired
    private lateinit var observationRegistry: ObservationRegistry

    @Autowired
    private lateinit var tracer: Tracer

    @Test
    fun `flights client propagates the current trace to flights-service`() {
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        val client = FlightsClient("http://flights", restClientBuilder)

        Observation.createNotStarted("incoming-request", observationRegistry).observe {
            val traceId = tracer.currentSpan()?.context()?.traceId()
            assertNotNull(traceId, "a span should be active during the observation")

            server
                .expect(requestTo("http://flights/flights/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("traceparent", matchesPattern("00-$traceId-[0-9a-f]{16}-0[01]")))
                .andRespond(
                    withSuccess(
                        """
                        {"id":1,"airline":"Demo","flightDate":"2026-12-01","departureTime":"08:00:00",
                        "origin":"BUE","destination":"PAR","capacity":180,"availability":100}
                        """.trimIndent(),
                        MediaType.APPLICATION_JSON,
                    ),
                )

            client.findById(1)
        }

        server.verify()
    }
}
