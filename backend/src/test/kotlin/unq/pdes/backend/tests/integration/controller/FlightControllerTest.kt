package unq.pdes.backend.tests.integration.controller

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import unq.pdes.backend.controller.dtos.requests.FlightRequestDto
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.external.flights.ExternalFlightRequestDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.external.flights.FlightsServiceUnavailableException
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
@WithMockUser(roles = ["BUYER"])
class FlightControllerTest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @MockitoBean
    private lateinit var flightsClient: FlightsClient

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        mvc =
            MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
        factory.cityWith("BUE", "Buenos Aires")
        factory.cityWith("PAR", "Paris")
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    private fun postFlight(body: String): ResultActions =
        mvc.perform(post("/flights").contentType(MediaType.APPLICATION_JSON).content(body))

    private fun postFlight(request: FlightRequestDto): ResultActions = postFlight(objectMapper.writeValueAsString(request))

    private fun requestWith(
        airline: String = "Aerolineas Argentinas",
        origin: String = "BUE",
        destination: String = "PAR",
        capacity: Int = 180,
        availability: Int = 180,
    ) = FlightRequestDto(airline, LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), origin, destination, capacity, availability)

    @Test
    fun `01 - GET flights as a buyer should return the flights found`() {
        Mockito.`when`(flightsClient.search("BUE", "PAR", LocalDate.of(2026, 12, 1))).thenReturn(listOf(flight(7L)))

        mvc
            .perform(get("/flights").param("origin", "bue").param("destination", "PAR").param("date", "2026-12-01"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].airline").value("Aerolineas Argentinas"))
            .andExpect(jsonPath("$[0].flightDate").value("2026-12-01"))
            .andExpect(jsonPath("$[0].origin").value("BUE"))
            .andExpect(jsonPath("$[0].availability").value(180))
    }

    @Test
    @WithMockUser(roles = ["AGENCY"])
    fun `02 - GET flights without filters should treat blank params as absent`() {
        Mockito.`when`(flightsClient.search(null, null, null)).thenReturn(listOf(flight(7L), flight(8L)))

        mvc
            .perform(get("/flights").param("origin", " ").param("destination", ""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `03 - GET flights with an invalid date should return 400`() {
        mvc
            .perform(get("/flights").param("date", "not-a-date"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("Invalid value for parameter 'date'."))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `04 - GET flights should return 503 when the flights service is unavailable`() {
        Mockito
            .`when`(
                flightsClient.search(null, null, null),
            ).thenThrow(FlightsServiceUnavailableException("Flights service is unavailable."))

        mvc
            .perform(get("/flights"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.errorData.description").value("Flights service is unavailable."))
    }

    @Test
    fun `05 - GET flight by id should return the flight`() {
        Mockito.`when`(flightsClient.findById(7L)).thenReturn(flight(7L))

        mvc
            .perform(get("/flights/{id}", 7))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.destination").value("PAR"))
    }

    @Test
    fun `06 - GET flight by id should return 404 when it does not exist`() {
        Mockito.`when`(flightsClient.findById(999L)).thenThrow(EntityNotFoundException("There is no Flight with id: 999."))

        mvc
            .perform(get("/flights/{id}", 999))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorData.description").value("There is no Flight with id: 999."))
    }

    @Test
    @WithAnonymousUser
    fun `07 - GET flights without authentication should be rejected`() {
        mvc
            .perform(get("/flights"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `08 - POST flights as admin should create the flight and return 201`() {
        Mockito.`when`(flightsClient.create(expectedClientRequest())).thenReturn(flight(11L))

        postFlight(requestWith(origin = " bue "))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(11))
            .andExpect(jsonPath("$.origin").value("BUE"))
            .andExpect(jsonPath("$.destination").value("PAR"))
            .andExpect(jsonPath("$.capacity").value(180))
    }

    @Test
    fun `09 - POST flights as buyer should return 403`() {
        postFlight(requestWith()).andExpect(status().isForbidden)
        Mockito.verifyNoInteractions(flightsClient)
    }

    @Test
    @WithMockUser(roles = ["AGENCY"])
    fun `10 - POST flights as agency should return 403`() {
        postFlight(requestWith()).andExpect(status().isForbidden)
        Mockito.verifyNoInteractions(flightsClient)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `11 - POST flights with a blank airline should return 400`() {
        postFlight(requestWith(airline = " "))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.httpCode").value(400))
            .andExpect(jsonPath("$.errorData.description").value("The flight must have an airline."))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `12 - POST flights with a non positive capacity should return 400`() {
        postFlight(requestWith(capacity = 0))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("The flight capacity must be greater than zero."))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `13 - POST flights with a negative availability should return 400`() {
        postFlight(requestWith(availability = -1))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("The flight availability cannot be negative."))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `14 - POST flights with availability above capacity should return 400`() {
        postFlight(requestWith(capacity = 10, availability = 11))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("Availability cannot exceed capacity."))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `15 - POST flights with an unknown city should return 400`() {
        postFlight(requestWith(destination = "ZZZ"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("There is no City with code: ZZZ."))
        postFlight(requestWith(origin = "YYY"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("There is no City with code: YYY."))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `16 - POST flights with the same origin and destination should return 400`() {
        postFlight(requestWith(destination = "BUE"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("Origin and destination must be different."))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `17 - POST flights with a malformed body should return 400`() {
        postFlight("""{"airline":"Aerolineas Argentinas","flightDate":"2026-13-45"}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.httpCode").value(400))
            .andExpect(jsonPath("$.errorData.description").value("The request body is malformed or incomplete."))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `18 - POST flights should return 400 when the flights service rejects the flight`() {
        Mockito.`when`(flightsClient.create(expectedClientRequest())).thenThrow(IllegalArgumentException("Flight rejected."))

        postFlight(requestWith())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("Flight rejected."))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `19 - POST flights should return 503 when the flights service is unavailable`() {
        val unavailable = FlightsServiceUnavailableException("Flights service is unavailable.")
        Mockito.`when`(flightsClient.create(expectedClientRequest())).thenThrow(unavailable)

        postFlight(requestWith())
            .andExpect(status().isServiceUnavailable)
    }

    private fun expectedClientRequest() =
        ExternalFlightRequestDto("Aerolineas Argentinas", LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), "BUE", "PAR", 180, 180)

    private fun flight(id: Long): ExternalFlightDto =
        ExternalFlightDto(
            id = id,
            airline = "Aerolineas Argentinas",
            flightDate = LocalDate.of(2026, 12, 1),
            departureTime = LocalTime.of(8, 0),
            origin = "BUE",
            destination = "PAR",
            capacity = 180,
            availability = 180,
        )
}
