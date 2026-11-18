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
        mvc =
            MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
        Mockito
            .`when`(flightsClient.sell(1L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(10L, 1L, "Bruno Buyer"))
        Mockito
            .`when`(flightsClient.sell(2L, "Bruno Buyer"))
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

        mvc
            .perform(
                post("/purchases")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PurchaseRequestDto(travelPackage.id!!))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.buyer.username").value("buyer"))
            .andExpect(jsonPath("$.travelPackage.name").value("París Romántico"))
            .andExpect(jsonPath("$.travelPackage.agency.name").value("Despegar"))
            .andExpect(jsonPath("$.travelPackage.hotel.name").value("Some hotel name"))
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `02 - GET purchases me should return the buyer purchases`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        mvc
            .perform(get("/purchases/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].travelPackage.name").value("París Romántico"))
            .andExpect(jsonPath("$.content[0].travelPackage.hotel.name").value("Some hotel name"))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `03 - GET purchases agency should return agency sales`() {
        factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        mvc
            .perform(get("/purchases/agency"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].buyer.username").value("buyer"))
            .andExpect(jsonPath("$.content[0].buyer.firstName").value("Bruno"))
            .andExpect(jsonPath("$.content[0].travelPackage.name").value("París Romántico"))
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `02a - GET purchases me should return only the requested page`() {
        val buyer = factory.buyerNamed("buyer")
        val first = factory.packageNamed("París Romántico")
        val second = factory.packageNamed("Londres Clásico", destinationCode = "LON", destinationCity = "London")
        purchaseService.purchase(buyer.username, first.id!!)
        purchaseService.purchase(buyer.username, second.id!!)

        mvc
            .perform(get("/purchases/me").param("page", "0").param("size", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.size").value(1))
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `02b - GET purchased package check should return whether package was bought`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        mvc
            .perform(get("/purchases/me/packages/${travelPackage.id}"))
            .andExpect(status().isOk)
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                    .content()
                    .string("true"),
            )
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `03a - GET agency sales should filter by buyer and package`() {
        factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        val matching = factory.packageNamed("París Romántico")
        val other = factory.packageNamed("Londres Clásico", destinationCode = "LON", destinationCity = "London")
        purchaseService.purchase(buyer.username, matching.id!!)
        purchaseService.purchase(buyer.username, other.id!!)

        mvc
            .perform(get("/purchases/agency").param("buyerUsername", "buyer").param("packageName", "románt"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].travelPackage.name").value("París Romántico"))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `04 - POST purchases as agency should return 403`() {
        factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("París Romántico")

        mvc
            .perform(
                post("/purchases")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PurchaseRequestDto(travelPackage.id!!))),
            ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `05 - GET purchases me as agency should return 403`() {
        factory.agencyUserNamed("agency")

        mvc
            .perform(get("/purchases/me"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `06 - GET purchases agency as buyer should return 403`() {
        factory.buyerNamed("buyer")

        mvc
            .perform(get("/purchases/agency"))
            .andExpect(status().isForbidden)
    }
}
