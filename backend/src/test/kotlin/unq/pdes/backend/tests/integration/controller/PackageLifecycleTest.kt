package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
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
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.Review
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.persistence.jpa.TravelPackageRepository
import unq.pdes.backend.service.JwtService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest
class PackageLifecycleTest {
    @Autowired private lateinit var purchases: PurchaseRepository

    @Autowired private lateinit var reviews: ReviewRepository

    @Autowired private lateinit var packages: TravelPackageRepository

    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var factory: PersistentObjectsFactory

    @Autowired private lateinit var jwt: JwtService

    @Autowired private lateinit var cleanup: DataServiceH2

    @MockitoBean private lateinit var flights: FlightsClient
    private lateinit var mvc: MockMvc
    private lateinit var admin: String
    private lateinit var buyer: String
    private lateinit var seller: String
    private var id = 0L

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
        admin = jwt.generateToken(factory.adminNamed("admin"))
        buyer = jwt.generateToken(factory.buyerNamed("buyer"))
        seller = jwt.generateToken(factory.agencyUserNamed("seller"))
        id = factory.packageNamed("Lifecycle").id!!
        Mockito.`when`(flights.findById(1L)).thenReturn(flight(1L))
        Mockito.`when`(flights.findById(2L)).thenReturn(flight(2L))
    }

    @AfterEach fun teardown() = cleanup.deleteAll()

    private fun flight(id: Long) =
        ExternalFlightDto(
            id,
            "Demo",
            LocalDate.now().plusDays(30),
            LocalTime.NOON,
            if (id == 1L) "BUE" else "PAR",
            if (id == 1L) "PAR" else "BUE",
            100,
            100,
        )

    private fun status(
        token: String,
        active: Boolean,
    ) = mvc.perform(
        put(
            "/packages/$id/status",
        ).header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"active":$active}"""),
    )

    @Test
    fun `archiving hides a package from catalog blocks purchases and preserves direct access and admin inventory`() {
        status(admin, false).andExpect(status().isOk).andExpect(jsonPath("$.active").value(false))
        mvc
            .perform(get("/packages?name=Lifecycle").header("Authorization", "Bearer $buyer"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
        mvc
            .perform(get("/admin/packages?active=false").header("Authorization", "Bearer $admin"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
        mvc
            .perform(get("/packages/$id").header("Authorization", "Bearer $buyer"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.active").value(false))
        mvc
            .perform(
                post(
                    "/purchases",
                ).header("Authorization", "Bearer $buyer").contentType(MediaType.APPLICATION_JSON).content("""{"packageId":$id}"""),
            ).andExpect(status().isBadRequest)
        Mockito.verify(flights, Mockito.never()).sell(Mockito.anyLong(), Mockito.anyString())
        status(seller, true).andExpect(status().isOk)
        mvc
            .perform(get("/packages?name=Lifecycle").header("Authorization", "Bearer $buyer"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `buyers and rival agencies cannot change package state or inspect admin inventory`() {
        val rival = jwt.generateToken(factory.agencyUserNamed("rival", "Rival"))
        status(buyer, false).andExpect(status().isForbidden)
        status(rival, false).andExpect(status().isForbidden)
        mvc.perform(get("/admin/packages").header("Authorization", "Bearer $seller")).andExpect(status().isForbidden)
    }

    @Test
    fun `archiving a sold package preserves purchases reviews and prevents physical deletion`() {
        val trip = packages.findById(id).orElseThrow()
        val buyer = factory.buyerNamed("buyer")
        val purchase =
            purchases.save(
                Purchase
                    .Builder()
                    .buyer(buyer)
                    .travelPackage(trip)
                    .agency(trip.agency)
                    .purchasePrice(BigDecimal("1000"))
                    .purchasedAt(LocalDateTime.now())
                    .build(),
            )
        reviews.save(
            Review
                .Builder()
                .buyer(buyer)
                .travelPackage(trip)
                .score(8)
                .comment("Demo")
                .build(),
        )
        status(admin, false).andExpect(status().isOk)
        val token = jwt.generateToken(buyer)
        mvc
            .perform(get("/purchases/${purchase.id}").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.purchasePrice").value(1000))
        mvc
            .perform(get("/packages/$id").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.reviews[0].score").value(8))
        mvc.perform(delete("/packages/$id").header("Authorization", "Bearer $admin")).andExpect(status().isBadRequest)
    }

    @Test
    fun `expired or sold out flights prevent publishing but never prevent archiving`() {
        status(admin, false).andExpect(status().isOk)
        Mockito.`when`(flights.findById(1L)).thenReturn(flight(1L).copy(availability = 0))
        status(admin, true).andExpect(status().isBadRequest)
        Mockito.`when`(flights.findById(1L)).thenReturn(flight(1L).copy(flightDate = LocalDate.now().minusDays(1)))
        status(admin, true).andExpect(status().isBadRequest)
        status(admin, false).andExpect(status().isOk)
    }
}
