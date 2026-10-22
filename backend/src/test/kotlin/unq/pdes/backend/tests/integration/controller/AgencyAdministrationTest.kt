package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Favorite
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.Review
import unq.pdes.backend.persistence.jpa.AgencyRepository
import unq.pdes.backend.persistence.jpa.FavoriteRepository
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import java.time.LocalDateTime

@SpringBootTest
class AgencyAdministrationTest {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var factory: PersistentObjectsFactory

    @Autowired private lateinit var cleanup: DataServiceH2

    @Autowired private lateinit var agencies: AgencyRepository

    @Autowired private lateinit var mapper: ObjectMapper

    @Autowired private lateinit var purchases: PurchaseRepository

    @Autowired private lateinit var favorites: FavoriteRepository

    @Autowired private lateinit var reviews: ReviewRepository
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @AfterEach
    fun clear() = cleanup.deleteAll()

    private val body = """{"name":"Nueva Agencia","username":"newagency","password":"secret123","firstName":"Ana","lastName":"Perez"}"""

    @Test
    fun `admin creates agency and account atomically and can rename it`() {
        val response =
            mvc
                .perform(
                    post("/agencies")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn()
        val id = mapper.readTree(response.response.contentAsString).get("id").asLong()
        mvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"username":"newagency","password":"secret123"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.user.role").value("AGENCY"))
        mvc
            .perform(
                put("/agencies/$id")
                    .with(user("admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Renombrada","active":true}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Renombrada"))
    }

    @Test
    fun `buyers agencies and anonymous callers cannot manage agencies`() {
        val agency = factory.agencyNamed("Despegar")
        for (role in listOf("BUYER", "AGENCY")) {
            mvc
                .perform(post("/agencies").with(user("u").roles(role)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden)
            mvc
                .perform(
                    put("/agencies/${agency.id}")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"Changed","active":false}"""),
                ).andExpect(status().isForbidden)
        }
        mvc.perform(post("/agencies").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden)
    }

    @Test
    fun `duplicate username rolls back agency creation and invalid input is rejected`() {
        factory.buyerNamed("newagency")
        mvc
            .perform(
                post("/agencies")
                    .with(user("admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)
        assertEquals(0, agencies.count())
        for (invalid in listOf(body.replace("Nueva Agencia", " "), body.replace("secret123", "123"))) {
            mvc
                .perform(
                    post("/agencies")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid),
                ).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `deactivation blocks old token login catalogue and direct purchases and reactivation restores access`() {
        val account = factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("Viaje")
        val buyer = factory.buyerNamed("buyer")
        val purchase =
            purchases.save(
                Purchase
                    .Builder()
                    .buyer(buyer)
                    .travelPackage(travelPackage)
                    .agency(account.agency)
                    .purchasePrice(travelPackage.price)
                    .purchasedAt(LocalDateTime.now())
                    .build(),
            )
        favorites.save(
            Favorite
                .Builder()
                .buyer(buyer)
                .travelPackage(travelPackage)
                .build(),
        )
        reviews.save(
            Review
                .Builder()
                .buyer(buyer)
                .travelPackage(travelPackage)
                .score(9)
                .comment("Excelente")
                .build(),
        )
        val login =
            mvc
                .perform(
                    post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"username":"agency","password":"agency123"}"""),
                ).andExpect(status().isOk)
                .andReturn()
        val token = mapper.readTree(login.response.contentAsString).get("token").asText()

        fun change(active: Boolean) {
            mvc
                .perform(
                    put("/agencies/${account.agency.id}")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"Despegar","active":$active}"""),
                ).andExpect(status().isOk)
        }
        change(false)
        assertEquals(1, purchases.count())
        assertEquals(1, favorites.count())
        assertEquals(1, reviews.count())
        mvc
            .perform(get("/purchases/me").with(user("buyer").roles("BUYER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(purchase.id))
        mvc.perform(get("/packages/agency").header("Authorization", "Bearer $token")).andExpect(status().isForbidden)
        mvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"username":"agency","password":"agency123"}"""),
            ).andExpect(status().isUnauthorized)
        mvc
            .perform(get("/packages").with(user("buyer").roles("BUYER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
        mvc
            .perform(
                post("/purchases")
                    .with(user("buyer").roles("BUYER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"packageId":${travelPackage.id}}"""),
            ).andExpect(status().isBadRequest)
        mvc
            .perform(get("/packages").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
        change(true)
        mvc.perform(get("/packages/agency").header("Authorization", "Bearer $token")).andExpect(status().isOk)
    }
}
