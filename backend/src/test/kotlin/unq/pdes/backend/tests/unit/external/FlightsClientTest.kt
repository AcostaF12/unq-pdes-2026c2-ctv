package unq.pdes.backend.tests.unit.external

import jakarta.persistence.EntityNotFoundException
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.external.flights.FlightsServiceUnavailableException

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

    @Test
    fun `03 - findById should throw EntityNotFoundException when the body is empty`() {
        server.expect(requestTo("http://flights/flights/7"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        val exception = assertThrows(EntityNotFoundException::class.java) {
            client.findById(7L)
        }

        assertEquals("There is no Flight with id: 7.", exception.message)
    }

    @Test
    fun `04 - findById should use status text when the error body has no description`() {
        server.expect(requestTo("http://flights/flights/7"))
            .andRespond(withResourceNotFound().body("{}").contentType(MediaType.APPLICATION_JSON))

        val exception = assertThrows(EntityNotFoundException::class.java) {
            client.findById(7L)
        }

        assertEquals("Not Found", exception.message)
    }

    @Test
    fun `05 - findById should ignore a truncated description in the error body`() {
        server.expect(requestTo("http://flights/flights/7"))
            .andRespond(
                withResourceNotFound().body("""{"errorData":{"description":"broken}}""")
                    .contentType(MediaType.APPLICATION_JSON),
            )

        val exception = assertThrows(EntityNotFoundException::class.java) {
            client.findById(7L)
        }

        assertEquals("Not Found", exception.message)
    }

    @Test
    fun `06 - findById should map 4xx errors to IllegalArgumentException`() {
        server.expect(requestTo("http://flights/flights/7"))
            .andRespond(
                withBadRequest().body(
                    """{"errorData":{"description":"Invalid flight id."}}""",
                ).contentType(MediaType.APPLICATION_JSON),
            )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            client.findById(7L)
        }

        assertEquals("Invalid flight id.", exception.message)
    }

    @Test
    fun `07 - findById should map 5xx errors to FlightsServiceUnavailableException`() {
        server.expect(requestTo("http://flights/flights/7"))
            .andRespond(
                withServerError().body(
                    """{"errorData":{"description":"Flights exploded."}}""",
                ).contentType(MediaType.APPLICATION_JSON),
            )

        val exception = assertThrows(FlightsServiceUnavailableException::class.java) {
            client.findById(7L)
        }

        assertEquals("Flights exploded.", exception.message)
    }

    @Test
    fun `08 - findById should map connectivity errors to FlightsServiceUnavailableException`() {
        server.expect(requestTo("http://flights/flights/7"))
            .andRespond(withException(IOException("I/O error")))

        val exception = assertThrows(FlightsServiceUnavailableException::class.java) {
            client.findById(7L)
        }

        assertEquals("Flights service is unavailable.", exception.message)
    }

    @Test
    fun `09 - sell should map the sale payload`() {
        server.expect(requestTo("http://flights/flights/1/sales"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{"id":10,"flightId":1,"passengerName":"Bruno Buyer"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val sale = client.sell(1L, "Bruno Buyer")

        assertEquals(10L, sale.id)
        assertEquals(1L, sale.flightId)
        assertEquals("Bruno Buyer", sale.passengerName)
        server.verify()
    }

    @Test
    fun `10 - sell should throw when the body is empty`() {
        server.expect(requestTo("http://flights/flights/1/sales"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        val exception = assertThrows(FlightsServiceUnavailableException::class.java) {
            client.sell(1L, "Bruno Buyer")
        }

        assertEquals("Could not sell a seat for flight 1.", exception.message)
    }

    @Test
    fun `11 - cancelSale should call the flights service`() {
        server.expect(requestTo("http://flights/flights/sales/10"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(HttpStatus.NO_CONTENT))

        client.cancelSale(10L)

        server.verify()
    }

    @Test
    fun `12 - findById should use the fallback message when description is blank`() {
        server.expect(requestTo("http://flights/flights/7"))
            .andRespond(
                withResourceNotFound().body("""{"errorData":{"description":""}}""")
                    .contentType(MediaType.APPLICATION_JSON),
            )

        val exception = assertThrows(EntityNotFoundException::class.java) {
            client.findById(7L)
        }

        assertEquals("There is no Flight with id: 7.", exception.message)
    }
}
