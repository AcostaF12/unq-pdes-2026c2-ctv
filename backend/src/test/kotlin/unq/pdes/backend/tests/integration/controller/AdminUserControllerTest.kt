package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
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
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.service.JwtService
import unq.pdes.backend.service.UserService
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
class AdminUserControllerTest {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var mapper: ObjectMapper

    @Autowired private lateinit var factory: PersistentObjectsFactory

    @Autowired private lateinit var cleanup: DataServiceH2

    @Autowired private lateinit var users: UserService

    @Autowired private lateinit var jwt: JwtService

    @Autowired private lateinit var purchases: PurchaseRepository
    private lateinit var mvc: MockMvc
    private lateinit var adminToken: String

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
        adminToken = jwt.generateToken(factory.adminNamed("admin"))
    }

    @AfterEach
    fun teardown() = cleanup.deleteAll()

    private fun createBody(
        role: String = "BUYER",
        agencyId: Long? = null,
    ): Map<String, Any?> =
        mapOf(
            "username" to "newuser",
            "password" to "secret123",
            "firstName" to " Ana ",
            "lastName" to " Pérez ",
            "role" to role,
            "agencyId" to agencyId,
        )

    private fun updateBody(
        role: String = "BUYER",
        enabled: Boolean = true,
        agencyId: Long? = null,
    ): Map<String, Any?> = mapOf("firstName" to "Ana", "lastName" to "Pérez", "role" to role, "enabled" to enabled, "agencyId" to agencyId)

    private fun update(
        id: Long,
        body: Map<String, Any?>,
    ) = mvc.perform(
        put(
            "/admin/users/$id",
        ).header(
            "Authorization",
            "Bearer $adminToken",
        ).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(body)),
    )

    @Test
    fun `admin lists users paginated without exposing password hashes`() {
        factory.buyerNamed("buyer")
        mvc
            .perform(get("/admin/users?size=1").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].password").doesNotExist())
            .andExpect(jsonPath("$.content[0].enabled").value(true))
        mvc.perform(get("/admin/users?size=101").header("Authorization", "Bearer $adminToken")).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = ["BUYER", "AGENCY", "ADMIN"])
    fun `admin creates each role and new account can log in`(role: String) {
        val agencyId = if (role == "AGENCY") factory.agencyNamed("New Agency").id else null
        mvc
            .perform(
                post(
                    "/admin/users",
                ).header(
                    "Authorization",
                    "Bearer $adminToken",
                ).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(createBody(role, agencyId))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.role").value(role))
            .andExpect(jsonPath("$.firstName").value("Ana"))
            .andExpect(jsonPath("$.password").doesNotExist())
        mvc
            .perform(
                post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""{"username":"newuser","password":"secret123"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.user.role").value(role))
    }

    @ParameterizedTest
    @ValueSource(strings = ["buyer", "seller"])
    fun `other roles cannot read create or update managed users or agencies`(username: String) {
        val account = if (username == "buyer") factory.buyerNamed(username) else factory.agencyUserNamed(username)
        val token = jwt.generateToken(account)
        mvc.perform(get("/admin/users").header("Authorization", "Bearer $token")).andExpect(status().isForbidden)
        mvc
            .perform(
                post(
                    "/admin/users",
                ).header(
                    "Authorization",
                    "Bearer $token",
                ).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(createBody())),
            ).andExpect(status().isForbidden)
        mvc
            .perform(
                put(
                    "/admin/users/${account.id}",
                ).header(
                    "Authorization",
                    "Bearer $token",
                ).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(updateBody("ADMIN"))),
            ).andExpect(status().isForbidden)
        mvc
            .perform(
                post(
                    "/agencies",
                ).header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"name":"Forbidden"}"""),
            ).andExpect(status().isForbidden)
        mvc
            .perform(
                put(
                    "/agencies/1",
                ).header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"name":"Forbidden"}"""),
            ).andExpect(status().isForbidden)
        mvc.perform(get("/admin/sales").header("Authorization", "Bearer $token")).andExpect(status().isForbidden)
    }

    @Test
    fun `anonymous cannot manage users`() {
        mvc.perform(get("/admin/users")).andExpect(status().isForbidden)
    }

    @Test
    fun `role transitions preserve identity and refresh authorities with existing token`() {
        val buyer = factory.buyerNamed("buyer")
        val token = jwt.generateToken(buyer)
        val agency = factory.agencyNamed("New Agency")
        update(
            buyer.id!!,
            updateBody("AGENCY", agencyId = agency.id),
        ).andExpect(status().isOk).andExpect(jsonPath("$.agency.id").value(agency.id))
        assertTrue(users.findByUsername("buyer") is AgencyUser)
        mvc.perform(get("/packages/agency").header("Authorization", "Bearer $token")).andExpect(status().isOk)
        mvc.perform(get("/purchases/me").header("Authorization", "Bearer $token")).andExpect(status().isForbidden)
        update(buyer.id!!, updateBody("ADMIN")).andExpect(status().isOk).andExpect(jsonPath("$.agency").isEmpty)
        mvc.perform(get("/admin/users").header("Authorization", "Bearer $token")).andExpect(status().isOk)
        update(buyer.id!!, updateBody("BUYER")).andExpect(status().isOk)
        assertTrue(users.findByUsername("buyer") !is AgencyUser)
        mvc.perform(get("/admin/users").header("Authorization", "Bearer $token")).andExpect(status().isForbidden)
        mvc.perform(get("/purchases/me").header("Authorization", "Bearer $token")).andExpect(status().isOk)
    }

    @Test
    fun `disabling account rejects login and already issued tokens and enabling restores login`() {
        val buyer = factory.buyerNamed("buyer")
        val token = jwt.generateToken(buyer)
        update(buyer.id!!, updateBody(enabled = false)).andExpect(status().isOk).andExpect(jsonPath("$.enabled").value(false))
        mvc.perform(get("/users/me").header("Authorization", "Bearer $token")).andExpect(status().isUnauthorized)
        val login = """{"username":"buyer","password":"buyer123"}"""
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isUnauthorized)
        update(buyer.id!!, updateBody()).andExpect(status().isOk)
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isOk)
    }

    @Test
    fun `admin cannot disable or demote own account`() {
        val admin = users.findByUsername("admin")
        update(admin.id!!, updateBody("ADMIN", enabled = false)).andExpect(status().isBadRequest)
        update(admin.id!!, updateBody()).andExpect(status().isBadRequest)
        update(admin.id!!, updateBody("ADMIN")).andExpect(status().isOk)
    }

    @Test
    fun `invalid agency role combinations missing users and invalid input are rejected`() {
        val id = factory.buyerNamed("buyer").id!!
        val agency = factory.agencyNamed("Despegar")
        update(id, updateBody("AGENCY")).andExpect(status().isBadRequest)
        update(id, updateBody(agencyId = agency.id)).andExpect(status().isBadRequest)
        update(id, updateBody("AGENCY", agencyId = Long.MAX_VALUE)).andExpect(status().isNotFound)
        update(Long.MAX_VALUE, updateBody()).andExpect(status().isNotFound)
        update(id, updateBody().minus("enabled")).andExpect(status().isBadRequest)
        update(id, updateBody().plus("enabled" to null)).andExpect(status().isBadRequest)
        for (body in listOf(
            createBody("AGENCY"),
            createBody("ROOT"),
            createBody().plus("password" to "short"),
            createBody().plus("username" to "bad user"),
            createBody().plus(
                "firstName" to " ",
            ),
            createBody().plus("password" to "á".repeat(40)),
        )) {
            mvc
                .perform(
                    post(
                        "/admin/users",
                    ).header(
                        "Authorization",
                        "Bearer $adminToken",
                    ).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)),
                ).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `public registration cannot escalate role agency or enabled state`() {
        val body = createBody("ADMIN").plus("enabled" to false)
        mvc
            .perform(
                post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.user.role").value("BUYER"))
        assertTrue(users.findByUsername("newuser").enabled)
    }

    @Test
    fun `duplicate username is rejected`() {
        factory.buyerNamed("newuser")
        mvc
            .perform(
                post(
                    "/admin/users",
                ).header(
                    "Authorization",
                    "Bearer $adminToken",
                ).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(createBody())),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `admin creates and renames agency and seller sees only assigned agency`() {
        val response =
            mvc
                .perform(
                    post(
                        "/agencies",
                    ).header(
                        "Authorization",
                        "Bearer $adminToken",
                    ).contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":" New Agency "}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("New Agency"))
                .andReturn()
        val id = mapper.readTree(response.response.contentAsString).get("id").asLong()
        mvc
            .perform(
                put(
                    "/agencies/$id",
                ).header(
                    "Authorization",
                    "Bearer $adminToken",
                ).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Renamed Agency"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Renamed Agency"))
        mvc
            .perform(
                post(
                    "/agencies",
                ).header(
                    "Authorization",
                    "Bearer $adminToken",
                ).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"renamed agency"}"""),
            ).andExpect(status().isBadRequest)
        val seller = factory.agencyUserNamed("seller", "Despegar")
        factory.packageNamed("Original package")
        factory.packageNamed("New package", agencyName = "Renamed Agency")
        update(seller.id!!, updateBody("AGENCY", agencyId = id)).andExpect(status().isOk)
        mvc
            .perform(get("/packages/agency").header("Authorization", "Bearer ${jwt.generateToken(seller)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("New package"))
    }

    @Test
    fun `admin sees global sales filters and history survives role changes`() {
        val buyer = factory.buyerNamed("buyer")
        val trip = factory.packageNamed("Historical trip")
        val purchase =
            purchases.save(
                Purchase
                    .Builder()
                    .buyer(
                        buyer,
                    ).travelPackage(trip)
                    .agency(trip.agency)
                    .purchasePrice(BigDecimal("1000.00"))
                    .purchasedAt(LocalDateTime.now())
                    .build(),
            )
        update(buyer.id!!, updateBody("AGENCY", agencyId = trip.agency.id)).andExpect(status().isOk)
        mvc
            .perform(get("/admin/sales?buyerUsername=buyer&packageName=Historical").header("Authorization", "Bearer $adminToken"))
            .andExpect(
                status().isOk,
            ).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].buyer.role").value("BUYER"))
        mvc
            .perform(get("/admin/sales?buyerUsername=other").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
        mvc
            .perform(
                get("/admin/sales?from=2026-10-22&to=2026-10-01").header("Authorization", "Bearer $adminToken"),
            ).andExpect(status().isBadRequest)
        assertEquals(
            buyer.id,
            purchases
                .findById(purchase.id!!)
                .orElseThrow()
                .buyer.id,
        )
    }
}
