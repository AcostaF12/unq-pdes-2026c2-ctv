package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.User
import unq.pdes.backend.persistence.jpa.AgencyRepository
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.persistence.jpa.TravelPackageRepository
import java.time.LocalDateTime

@SpringBootTest
class AdminReportControllerTest {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var factory: PersistentObjectsFactory

    @Autowired private lateinit var cleanup: DataServiceH2

    @Autowired private lateinit var purchases: PurchaseRepository

    @Autowired private lateinit var reviews: ReviewRepository

    @Autowired private lateinit var packages: TravelPackageRepository

    @Autowired private lateinit var agencies: AgencyRepository

    @MockitoBean private lateinit var flights: FlightsClient
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @AfterEach
    fun clear() = cleanup.deleteAll()

    private fun purchase(
        buyer: User,
        travelPackage: TravelPackage,
    ) {
        purchases.save(
            Purchase
                .Builder()
                .buyer(buyer)
                .travelPackage(travelPackage)
                .agency(travelPackage.agency)
                .purchasePrice(travelPackage.price)
                .purchasedAt(LocalDateTime.now())
                .build(),
        )
    }

    @Test
    fun `only admins can read reports`() {
        mvc.perform(get("/admin/reports")).andExpect(status().isForbidden)
        for (role in listOf("BUYER", "AGENCY")) {
            mvc.perform(get("/admin/reports").with(user("u").roles(role))).andExpect(status().isForbidden)
        }
    }

    @Test
    fun `empty database returns four empty reports`() {
        mvc
            .perform(get("/admin/reports").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.topBuyers").isEmpty)
            .andExpect(jsonPath("$.topAgencies").isEmpty)
            .andExpect(jsonPath("$.topDestinations").isEmpty)
            .andExpect(jsonPath("$.topRatedDestinations").isEmpty)
        verifyNoInteractions(flights)
    }

    @Test
    fun `all rankings aggregate in descending order and return at most five rows`() {
        for (i in 1..7) {
            val buyer = factory.buyerNamed("buyer$i")
            val travelPackage =
                factory.packageNamed(
                    "Viaje$i",
                    destinationCode = "D0$i",
                    destinationCity = "Destino$i",
                    agencyName = "Agencia$i",
                )
            repeat(i) { purchase(buyer, travelPackage) }
            reviews.save(
                Review
                    .Builder()
                    .buyer(buyer)
                    .travelPackage(travelPackage)
                    .score(i)
                    .build(),
            )
        }
        mvc
            .perform(get("/admin/reports").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.topBuyers.length()").value(5))
            .andExpect(jsonPath("$.topBuyers[0].name").value("buyer7"))
            .andExpect(jsonPath("$.topBuyers[0].total").value(7))
            .andExpect(jsonPath("$.topBuyers[4].total").value(3))
            .andExpect(jsonPath("$.topAgencies.length()").value(5))
            .andExpect(jsonPath("$.topAgencies[0].name").value("Agencia7"))
            .andExpect(jsonPath("$.topAgencies[0].total").value(7))
            .andExpect(jsonPath("$.topDestinations.length()").value(5))
            .andExpect(jsonPath("$.topDestinations[0].code").value("D07"))
            .andExpect(jsonPath("$.topDestinations[0].total").value(7))
            .andExpect(jsonPath("$.topRatedDestinations.length()").value(5))
            .andExpect(jsonPath("$.topRatedDestinations[0].average").value(7.0))
        verifyNoInteractions(flights)
    }

    @Test
    fun `ratings average individual reviews across packages not package averages`() {
        val first = factory.packageNamed("Uno")
        val second = factory.packageNamed("Dos")
        reviews.save(
            Review
                .Builder()
                .buyer(factory.buyerNamed("a"))
                .travelPackage(first)
                .score(10)
                .build(),
        )
        reviews.save(
            Review
                .Builder()
                .buyer(factory.buyerNamed("b"))
                .travelPackage(first)
                .score(8)
                .build(),
        )
        reviews.save(
            Review
                .Builder()
                .buyer(factory.buyerNamed("c"))
                .travelPackage(second)
                .score(0)
                .build(),
        )
        mvc
            .perform(get("/admin/reports").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.topRatedDestinations[0].code").value("PAR"))
            .andExpect(jsonPath("$.topRatedDestinations[0].average").value(6.0))
            .andExpect(jsonPath("$.topRatedDestinations[0].reviewCount").value(3))
    }

    @Test
    fun `sold destinations use purchase snapshots and inactive agencies keep their sales`() {
        val travelPackage = factory.packageNamed("Original")
        purchase(factory.buyerNamed("buyer"), travelPackage)
        travelPackage.destination = factory.cityWith("ROM", "Roma")
        packages.save(travelPackage)
        val agency = agencies.findById(travelPackage.agency.id!!).orElseThrow()
        agency.active = false
        agencies.save(agency)
        mvc
            .perform(get("/admin/reports").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.topDestinations[0].code").value("PAR"))
            .andExpect(jsonPath("$.topAgencies[0].total").value(1))
        verifyNoInteractions(flights)
    }

    @Test
    fun `ties are deterministic and groups without activity are excluded`() {
        val travelPackage = factory.packageNamed("Viaje")
        purchase(factory.buyerNamed("zeta"), travelPackage)
        purchase(factory.buyerNamed("alpha"), travelPackage)
        factory.buyerNamed("unused")
        factory.agencyNamed("Unused")
        mvc
            .perform(get("/admin/reports").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.topBuyers.length()").value(2))
            .andExpect(jsonPath("$.topBuyers[0].name").value("alpha"))
            .andExpect(jsonPath("$.topAgencies.length()").value(1))
            .andExpect(jsonPath("$.topRatedDestinations").isEmpty)
    }
}
