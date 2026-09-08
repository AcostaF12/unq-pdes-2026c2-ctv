package unq.pdes.backend.tests.unit.external

import jakarta.persistence.EntityNotFoundException
import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import unq.pdes.backend.external.flights.FlightsClient

class FlightsClientTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var client: FlightsClient

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        client = FlightsClient("http://flights", builder)
    }

    @Test
    fun `01 - findById should map the flight payload`() {
        server.expect(requestTo("http://flights/flights/7"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    {
                      "id": 7,
                      "airline": "Aerolineas Argentinas",
                      "flightDate": "2026-12-01",
                      "departureTime": "08:00:00",
                      "origin": "BUE",
                      "destination": "PAR",
                      "capacity": 180,
                      "availability": 42
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val flight = client.findById(7L)

        assertEquals(7L, flight.id)
        assertEquals("BUE", flight.origin)
        assertEquals("PAR", flight.destination)
        assertEquals(LocalDate.of(2026, 12, 1), flight.flightDate)
        assertEquals(LocalTime.of(8, 0), flight.departureTime)
        assertEquals(42, flight.availability)
        server.verify()
    }

    @Test
    fun `02 - findById should throw EntityNotFoundException on 404`() {
        server.expect(requestTo("http://flights/flights/999"))
            .andRespond(
                withResourceNotFound().body(
                    """{"httpCode":404,"httpStatus":"NOT_FOUND","errorData":{"description":"There is no Flight with id: 999."}}""",
                ).contentType(MediaType.APPLICATION_JSON),
            )

        val exception = assertThrows(EntityNotFoundException::class.java) {
            client.findById(999L)
        }

        assertEquals("There is no Flight with id: 999.", exception.message)
    }
}
