package unq.pdes.flightsservice.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import unq.pdes.flightsservice.controller.dtos.requests.FlightRequestDto
import unq.pdes.flightsservice.helpers.factory.PersistentObjectsFactory
import unq.pdes.flightsservice.helpers.service.DataServiceH2
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
class FlightControllerTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    private fun anyRequest(): FlightRequestDto {
        return FlightRequestDto(
            airline = "Aerolineas Argentinas",
            flightDate = LocalDate.of(2026, 12, 1),
            departureTime = LocalTime.of(8, 0),
            origin = "BUE",
            destination = "PAR",
            capacity = 180,
            availability = 180,
        )
    }

    @Test
    fun `01 - POST vuelos should create the flight and return 201`() {
        mvc.perform(
            post("/flights").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anyRequest())),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.origin").value("BUE"))
            .andExpect(jsonPath("$.destination").value("PAR"))
            .andExpect(jsonPath("$.availability").value(180))
    }

    @Test
    fun `02 - POST vuelos with blank airline should return 400`() {
        val request = anyRequest().copy(airline = "  ")

        mvc.perform(
            post("/flights").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.httpCode").value(400))
            .andExpect(jsonPath("$.errorData.description").value("The flight must have an airline."))
    }

    @Test
    fun `03 - POST vuelos with same origin and destination should return 400`() {
        val request = anyRequest().copy(origin = "BUE", destination = "BUE")

        mvc.perform(
            post("/flights").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("Origin and destination must be different."))
    }

    @Test
    fun `04 - GET vuelos should return only flights with availability`() {
        factory.flightWithAvailability(0)
        factory.flightWithAvailability(30)

        mvc.perform(get("/flights"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].availability").value(30))
    }

    @Test
    fun `05 - GET vuelos should filter by destination`() {
        factory.flightFrom("BUE", "PAR")
        factory.flightFrom("BUE", "LON")

        mvc.perform(get("/flights").param("destination", "LON"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].destination").value("LON"))
    }

    @Test
    fun `06 - GET vuelos should filter by date`() {
        factory.flightOn(LocalDate.of(2026, 12, 25))
        factory.flightOn(LocalDate.of(2026, 12, 1))

        mvc.perform(get("/flights").param("date", "2026-12-25"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].flightDate").value("2026-12-25"))
    }
}
