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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import unq.pdes.backend.controller.dtos.requests.ChangePasswordRequestDto
import unq.pdes.backend.controller.dtos.requests.LoginRequestDto
import unq.pdes.backend.controller.dtos.requests.RegisterRequestDto
import unq.pdes.backend.controller.dtos.requests.UpdateProfileRequestDto
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

    @Test
    fun `04 - GET me with a malformed Authorization header should be rejected`() {
        mvc.perform(get("/users/me").header("Authorization", "Basic something"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `05 - GET me with an invalid bearer token should be rejected`() {
        mvc.perform(get("/users/me").header("Authorization", "Bearer not.a.valid.token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `06 - PATCH me should update first and last name`() {
        val token = registerAndGetToken("jdoe")

        mvc.perform(
            patch("/users/me")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateProfileRequestDto("Jane", "Roe"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Jane"))
            .andExpect(jsonPath("$.lastName").value("Roe"))
            .andExpect(jsonPath("$.username").value("jdoe"))
    }

    @Test
    fun `07 - PATCH me without a token should be rejected`() {
        mvc.perform(
            patch("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateProfileRequestDto("Jane", "Roe"))),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `08 - PUT password should accept a valid current password`() {
        val token = registerAndGetToken("jdoe")

        mvc.perform(
            put("/users/me/password")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ChangePasswordRequestDto("secret", "secret2"))),
        )
            .andExpect(status().isNoContent)

        mvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequestDto("jdoe", "secret2"))),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `09 - PUT password should reject an incorrect current password`() {
        val token = registerAndGetToken("jdoe")

        mvc.perform(
            put("/users/me/password")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ChangePasswordRequestDto("wrong", "secret2"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("The current password is incorrect."))
    }
}
