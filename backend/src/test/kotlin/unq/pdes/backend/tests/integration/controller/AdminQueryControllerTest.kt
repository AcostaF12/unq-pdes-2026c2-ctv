package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Favorite
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.Review
import unq.pdes.backend.persistence.jpa.FavoriteRepository
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.persistence.jpa.TravelPackageRepository
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
class AdminQueryControllerTest {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var factory: PersistentObjectsFactory

    @Autowired private lateinit var cleanup: DataServiceH2

    @Autowired private lateinit var favorites: FavoriteRepository

    @Autowired private lateinit var reviews: ReviewRepository

    @Autowired private lateinit var purchases: PurchaseRepository

    @Autowired private lateinit var packages: TravelPackageRepository
    private lateinit var mvc: MockMvc
    private val paths = listOf("users", "favorites", "reviews", "purchases")

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @AfterEach
    fun clear() = cleanup.deleteAll()

    @Test
    fun `all queries reject non admins and anonymous callers`() {
        for (path in paths) {
            mvc.perform(get("/admin/$path")).andExpect(status().isForbidden)
            for (role in listOf("BUYER", "AGENCY")) {
                mvc.perform(get("/admin/$path").with(user("u").roles(role))).andExpect(status().isForbidden)
            }
        }
    }

    @Test
    fun `users list includes every role without credentials`() {
        factory.buyerNamed("buyer")
        factory.agencyUserNamed("agency")
        factory.adminNamed("admin")
        mvc
            .perform(get("/admin/users").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].username").value("admin"))
            .andExpect(jsonPath("$.content[1].role").value("AGENCY"))
            .andExpect(jsonPath("$.content[1].agency.name").value("Despegar"))
            .andExpect(jsonPath("$.content[*].password").isEmpty)
    }

    @Test
    fun `favorites are paginated across buyers with identifiable packages`() {
        val travelPackage = factory.packageNamed("Viaje")
        val first =
            favorites.save(
                Favorite
                    .Builder()
                    .buyer(factory.buyerNamed("first"))
                    .travelPackage(travelPackage)
                    .build(),
            )
        val second =
            favorites.save(
                Favorite
                    .Builder()
                    .buyer(factory.buyerNamed("second"))
                    .travelPackage(travelPackage)
                    .build(),
            )
        mvc
            .perform(get("/admin/favorites").param("size", "1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content[0].id").value(second.id))
            .andExpect(jsonPath("$.content[0].buyerUsername").value("second"))
            .andExpect(jsonPath("$.content[0].packageName").value("Viaje"))
        mvc
            .perform(get("/admin/favorites").param("page", "1").param("size", "1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(first.id))
    }

    @Test
    fun `reviews show buyer package score and optional comment`() {
        val travelPackage = factory.packageNamed("Viaje")
        reviews.save(
            Review
                .Builder()
                .buyer(factory.buyerNamed("buyer"))
                .travelPackage(travelPackage)
                .score(8)
                .comment("Muy bueno")
                .build(),
        )
        mvc
            .perform(get("/admin/reviews").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].buyerUsername").value("buyer"))
            .andExpect(jsonPath("$.content[0].packageName").value("Viaje"))
            .andExpect(jsonPath("$.content[0].score").value(8))
            .andExpect(jsonPath("$.content[0].comment").value("Muy bueno"))
    }

    @Test
    fun `purchases retain historical price and package after the current package changes`() {
        val travelPackage = factory.packageNamed("Original")
        purchases.save(
            Purchase
                .Builder()
                .buyer(factory.buyerNamed("buyer"))
                .travelPackage(travelPackage)
                .agency(travelPackage.agency)
                .purchasePrice(BigDecimal("1234.00"))
                .purchasedAt(LocalDateTime.of(2026, 10, 20, 12, 0))
                .build(),
        )
        travelPackage.name = "Changed"
        travelPackage.price = BigDecimal("9999.00")
        packages.save(travelPackage)
        mvc
            .perform(get("/admin/purchases").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].buyer.username").value("buyer"))
            .andExpect(jsonPath("$.content[0].travelPackage.name").value("Original"))
            .andExpect(jsonPath("$.content[0].travelPackage.agency.name").value("Despegar"))
            .andExpect(jsonPath("$.content[0].purchasePrice").value(1234.0))
    }

    @Test
    fun `empty lists and invalid pages have predictable responses`() {
        for (path in paths) {
            mvc
                .perform(get("/admin/$path").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isEmpty)
            for ((parameter, value) in listOf("page" to "-1", "size" to "0", "size" to "101", "page" to "bad")) {
                mvc
                    .perform(get("/admin/$path").param(parameter, value).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isBadRequest)
            }
        }
    }
}
