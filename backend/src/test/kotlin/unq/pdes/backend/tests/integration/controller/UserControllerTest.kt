package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import unq.pdes.backend.controller.dtos.requests.LoginRequestDto
import unq.pdes.backend.controller.dtos.requests.RegisterRequestDto
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Agency
import unq.pdes.backend.persistence.jpa.AgencyRepository
import unq.pdes.backend.service.UserService

@SpringBootTest
class UserControllerTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var agencyRepository: AgencyRepository

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    private fun registerAndGetToken(username: String): String {
        val body = mvc.perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto(username, "secret", "John", "Doe"))),
        ).andReturn().response.contentAsString
        return objectMapper.readTree(body).get("token").asString()
    }

    private fun loginAndGetToken(username: String, password: String): String {
        val body = mvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequestDto(username, password))),
        ).andReturn().response.contentAsString
        return objectMapper.readTree(body).get("token").asString()
    }

    @Test
    fun `01 - GET me without a token should be rejected`() {
        mvc.perform(get("/users/me"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `02 - GET me with a valid token should return the current buyer`() {
        val token = registerAndGetToken("jdoe")

        mvc.perform(get("/users/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("jdoe"))
            .andExpect(jsonPath("$.role").value("BUYER"))
            .andExpect(jsonPath("$.agency").doesNotExist())
    }

    @Test
    fun `03 - GET me as an agency user should include the agency`() {
        val agency = agencyRepository.save(Agency.Builder().name("Despegar").build())
        userService.createAgencyUser("agency", "secret", "Agus", "Agency", agency)
        val token = loginAndGetToken("agency", "secret")

        mvc.perform(get("/users/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("AGENCY"))
            .andExpect(jsonPath("$.agency.name").value("Despegar"))
    }
}
