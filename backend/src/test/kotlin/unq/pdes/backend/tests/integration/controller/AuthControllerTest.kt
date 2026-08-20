package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
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

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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

    private fun register(username: String) {
        mvc.perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto(username, "secret", "John", "Doe"))),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `01 - POST register should create a buyer and return 201 with a token`() {
        mvc.perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto("jdoe", "secret", "John", "Doe"))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.user.username").value("jdoe"))
            .andExpect(jsonPath("$.user.role").value("BUYER"))
    }

    @Test
    fun `02 - POST login with valid credentials should return 200 with a token`() {
        register("jdoe")

        mvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequestDto("jdoe", "secret"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.user.username").value("jdoe"))
    }

    @Test
    fun `03 - POST login with wrong password should return 401`() {
        register("jdoe")

        mvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequestDto("jdoe", "wrong"))),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `04 - POST register with a taken username should return 400`() {
        register("jdoe")

        mvc.perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto("jdoe", "other", "Jane", "Doe"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("The username 'jdoe' is already taken."))
    }
}
