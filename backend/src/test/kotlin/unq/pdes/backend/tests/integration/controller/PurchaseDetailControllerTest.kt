package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.external.flights.FlightsServiceUnavailableException
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest
class PurchaseDetailControllerTest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @Autowired
    private lateinit var purchaseRepository: PurchaseRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @MockitoBean
    private lateinit var flightsClient: FlightsClient

    private lateinit var mvc: MockMvc
    private lateinit var travelPackage: TravelPackage
    private var purchaseId: Long = 0

    @BeforeEach
    fun setUp() {
        mvc =
            MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
        Mockito.`when`(flightsClient.findById(1L)).thenReturn(flight(1L, "BUE", "PAR"))
        Mockito.`when`(flightsClient.findById(2L)).thenReturn(flight(2L, "PAR", "BUE"))
        val buyer = factory.buyerNamed("buyer")
        travelPackage = factory.packageNamed("París Romántico")
        purchaseId =
            purchaseRepository
                .save(
                    Purchase
                        .Builder()
                        .buyer(buyer)
                        .travelPackage(travelPackage)
                        .agency(travelPackage.agency)
                        .purchasePrice(BigDecimal("1400.00"))
                        .purchasedAt(LocalDateTime.of(2026, 3, 10, 9, 0))
                        .build(),
                ).id!!
        reviewRepository.save(
            Review
                .Builder()
                .buyer(buyer)
                .travelPackage(travelPackage)
                .score(9)
                .comment("Excelente")
                .build(),
        )
        factory.agencyUserNamed("agency")
        factory.agencyUserNamed("rival", "Almundo")
        factory.buyerNamed("other")
        factory.adminNamed("admin")
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `01 - GET purchase detail as its buyer should include flights and the review`() {
        mvc
            .perform(get("/purchases/{id}", purchaseId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(purchaseId))
            .andExpect(jsonPath("$.purchasePrice").value(1400.00))
            .andExpect(jsonPath("$.travelPackage.name").value("París Romántico"))
            .andExpect(jsonPath("$.buyer.username").value("buyer"))
            .andExpect(jsonPath("$.outboundFlight.origin").value("BUE"))
            .andExpect(jsonPath("$.returnFlight.origin").value("PAR"))
            .andExpect(jsonPath("$.review.score").value(9))
            .andExpect(jsonPath("$.review.comment").value("Excelente"))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `02 - GET purchase detail as the selling agency should not expose the buyer review`() {
        mvc
            .perform(get("/purchases/{id}", purchaseId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.buyer.username").value("buyer"))
            .andExpect(jsonPath("$.outboundFlight.id").value(1))
            .andExpect(jsonPath("$.review").doesNotExist())
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `03 - GET purchase detail should return null flights when the flights service fails`() {
        Mockito.`when`(flightsClient.findById(1L)).thenThrow(FlightsServiceUnavailableException("down"))
        Mockito.`when`(flightsClient.findById(2L)).thenThrow(FlightsServiceUnavailableException("down"))

        mvc
            .perform(get("/purchases/{id}", purchaseId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.outboundFlight").doesNotExist())
            .andExpect(jsonPath("$.returnFlight").doesNotExist())
            .andExpect(jsonPath("$.travelPackage.name").value("París Romántico"))
    }

    @Test
    @WithMockUser(username = "other", roles = ["BUYER"])
    fun `04 - GET purchase detail as another buyer should return 403`() {
        mvc.perform(get("/purchases/{id}", purchaseId)).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "rival", roles = ["AGENCY"])
    fun `05 - GET purchase detail as another agency should return 403`() {
        mvc.perform(get("/purchases/{id}", purchaseId)).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `06 - GET purchase detail as admin should show the sale`() {
        mvc.perform(get("/purchases/{id}", purchaseId)).andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `07 - GET purchase detail of an unknown purchase should return 404`() {
        mvc.perform(get("/purchases/{id}", 99999L)).andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `08 - GET purchases me should apply filters and sorting`() {
        mvc
            .perform(
                get("/purchases/me")
                    .param("packageName", "romántico")
                    .param("destination", "PAR")
                    .param("from", "2026-03-10")
                    .param("to", "2026-03-10")
                    .param("minPrice", "1400")
                    .param("maxPrice", "1400")
                    .param("sort", "price,asc"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].travelPackage.name").value("París Romántico"))

        mvc
            .perform(get("/purchases/me").param("destination", "LON"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    @WithMockUser(username = "buyer", roles = ["BUYER"])
    fun `09 - GET purchases me with an invalid sort or an inverted range should return 400`() {
        mvc.perform(get("/purchases/me").param("sort", "name,asc")).andExpect(status().isBadRequest)
        mvc
            .perform(get("/purchases/me").param("from", "2026-05-01").param("to", "2026-04-01"))
            .andExpect(status().isBadRequest)
        mvc
            .perform(get("/purchases/me").param("minPrice", "10").param("maxPrice", "5"))
            .andExpect(status().isBadRequest)
    }

    private fun flight(
        id: Long,
        origin: String,
        destination: String,
    ) = ExternalFlightDto(id, "Aerolineas Argentinas", LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), origin, destination, 180, 150)
}
