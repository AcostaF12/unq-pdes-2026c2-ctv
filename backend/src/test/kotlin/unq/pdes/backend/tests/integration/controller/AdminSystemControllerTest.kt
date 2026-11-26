package unq.pdes.backend.tests.integration.controller

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

@SpringBootTest(
    properties = [
        "resilience4j.circuitbreaker.instances.flightsService.sliding-window-size=10",
    ],
)
class AdminSystemControllerTest {
    @Autowired private lateinit var context: WebApplicationContext

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @Test
    fun `a buyer cannot read the system snapshot`() {
        mvc.perform(get("/admin/system").with(user("buyer").roles("BUYER"))).andExpect(status().isForbidden)
    }

    @Test
    fun `an admin reads database status and the flights circuit breaker`() {
        mvc
            .perform(get("/admin/system").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.capturedAt").exists())
            .andExpect(jsonPath("$.systems[0].name").value("backend"))
            .andExpect(jsonPath("$.systems[0].status").value("UP"))
            .andExpect(jsonPath("$.systems[1].name").value("database"))
            .andExpect(jsonPath("$.systems[1].status").value("UP"))
            .andExpect(jsonPath("$.systems[2].name").value("flights"))
            .andExpect(jsonPath("$.systems[2].status").value("UNKNOWN"))
            .andExpect(jsonPath("$.circuitBreaker.name").value("flightsService"))
            .andExpect(jsonPath("$.circuitBreaker.state").value("CLOSED"))
            .andExpect(jsonPath("$.http.requestCount").isNumber())
            .andExpect(jsonPath("$.concurrency").exists())
    }
}
