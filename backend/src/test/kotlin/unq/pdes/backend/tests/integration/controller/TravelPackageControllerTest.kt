package unq.pdes.backend.tests.integration.controller

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import unq.pdes.backend.controller.dtos.requests.TravelPackageRequestDto
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2

@SpringBootTest
@WithMockUser(roles = ["BUYER"])
class TravelPackageControllerTest {

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
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
        Mockito.`when`(flightsClient.findById(1L)).thenReturn(flight(1L, "BUE", "PAR"))
        Mockito.`when`(flightsClient.findById(2L)).thenReturn(flight(2L, "PAR", "BUE"))
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - GET packages should return persisted packages`() {
        factory.packageNamed("París Romántico")

        mvc.perform(get("/packages"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("París Romántico"))
            .andExpect(jsonPath("$[0].origin.code").value("BUE"))
            .andExpect(jsonPath("$[0].destination.code").value("PAR"))
    }

    @Test
    fun `02 - GET package by id should return 404 when missing`() {
        mvc.perform(get("/packages/{id}", 999))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorData.description").value("There is no TravelPackage with id: 999."))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `03 - POST packages as agency should create the package`() {
        factory.cityWith("BUE", "Buenos Aires")
        val hotel = factory.hotelIn(factory.cityWith("PAR", "Paris"))
        factory.agencyUserNamed("agency")
        val request = TravelPackageRequestDto("París Romántico", hotel.id!!, 1L, 2L, BigDecimal("1500.00"))

        mvc.perform(
            post("/packages").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("París Romántico"))
            .andExpect(jsonPath("$.origin.code").value("BUE"))
    }

    @Test
    fun `04 - POST packages as buyer should return 403`() {
        factory.cityWith("PAR", "Paris")
        val request = TravelPackageRequestDto("París Romántico", 1L, 1L, 2L, BigDecimal("1500.00"))

        mvc.perform(
            post("/packages").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `05 - GET package by id should include flights`() {
        val travelPackage = factory.packageNamed("París Romántico")

        mvc.perform(get("/packages/{id}", travelPackage.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("París Romántico"))
            .andExpect(jsonPath("$.outboundFlight.origin").value("BUE"))
            .andExpect(jsonPath("$.returnFlight.origin").value("PAR"))
            .andExpect(jsonPath("$.reviews.length()").value(0))
    }

    @Test
    fun `06 - GET package by id should tolerate unavailable flights`() {
        val travelPackage = factory.packageNamed("París Romántico")
        Mockito.`when`(flightsClient.findById(1L)).thenThrow(RuntimeException("down"))
        Mockito.`when`(flightsClient.findById(2L)).thenThrow(RuntimeException("down"))

        mvc.perform(get("/packages/{id}", travelPackage.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("París Romántico"))
            .andExpect(jsonPath("$.outboundFlight").doesNotExist())
            .andExpect(jsonPath("$.returnFlight").doesNotExist())
    }

    @Test
    fun `07 - GET packages should filter by name origin and destination`() {
        factory.packageNamed("París Romántico")
        factory.packageNamed("Londres Clásico", destinationCode = "LON", destinationCity = "London")

        mvc.perform(
            get("/packages")
                .param("name", "París")
                .param("origin", "BUE")
                .param("destination", "PAR"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("París Romántico"))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `08 - GET agency packages should return owned packages`() {
        factory.agencyUserNamed("agency")
        factory.packageNamed("París Romántico")

        mvc.perform(get("/packages/agency"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("París Romántico"))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `09 - PUT packages should update the package`() {
        factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("París Romántico")
        val request = TravelPackageRequestDto(
            "París Premium",
            travelPackage.hotel.id!!,
            1L,
            2L,
            BigDecimal("1800.00"),
        )

        mvc.perform(
            put("/packages/{id}", travelPackage.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("París Premium"))
            .andExpect(jsonPath("$.price").value(1800.00))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `10 - DELETE packages should remove the package`() {
        factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("París Romántico")

        mvc.perform(delete("/packages/{id}", travelPackage.id))
            .andExpect(status().isNoContent)

        mvc.perform(get("/packages/{id}", travelPackage.id))
            .andExpect(status().isNotFound)
    }

    private fun flight(id: Long, origin: String, destination: String): ExternalFlightDto {
        return ExternalFlightDto(
            id = id,
            airline = "Aerolineas Argentinas",
            flightDate = LocalDate.of(2026, 12, 1),
            departureTime = LocalTime.of(8, 0),
            origin = origin,
            destination = destination,
            capacity = 180,
            availability = 180,
        )
    }
}
