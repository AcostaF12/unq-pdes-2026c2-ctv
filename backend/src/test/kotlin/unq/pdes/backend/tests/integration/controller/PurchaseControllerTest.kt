package unq.pdes.backend.tests.integration.controller

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import unq.pdes.backend.controller.dtos.requests.PurchaseRequestDto
import unq.pdes.backend.external.flights.ExternalFlightSaleDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.service.PurchaseService

@SpringBootTest
class PurchaseControllerTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var purchaseService: PurchaseService

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
        Mockito.`when`(flightsClient.sell(1L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(10L, 1L, "Bruno Buyer"))
        Mockito.`when`(flightsClient.sell(2L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(11L, 2L, "Bruno Buyer"))
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `01 - POST purchases should create a purchase`() {
        factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")

        mvc.perform(
            post("/purchases").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(PurchaseRequestDto(travelPackage.id!!))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.packageName").value("París Romántico"))
            .andExpect(jsonPath("$.agency.name").value("Despegar"))
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `02 - GET purchases me should return the buyer purchases`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        mvc.perform(get("/purchases/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].packageName").value("París Romántico"))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `03 - GET purchases agency should return agency sales`() {
        factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        mvc.perform(get("/purchases/agency"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].packageName").value("París Romántico"))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `04 - POST purchases as agency should return 403`() {
        factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("París Romántico")

        mvc.perform(
            post("/purchases").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(PurchaseRequestDto(travelPackage.id!!))),
        )
            .andExpect(status().isForbidden)
    }
}
