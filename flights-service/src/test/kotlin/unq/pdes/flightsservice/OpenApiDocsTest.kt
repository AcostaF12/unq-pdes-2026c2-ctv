package unq.pdes.flightsservice

import kotlin.test.assertContains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `api-docs endpoint should expose the OpenAPI spec`() {
        val body = mockMvc.get("/v3/api-docs")
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString

        assertContains(body, "\"openapi\"")
        assertContains(body, "Flights Service API")
    }

    @Test
    fun `swagger-ui should redirect to the index page`() {
        mockMvc.get("/swagger-ui.html")
            .andExpect { status { is3xxRedirection() } }
    }
}
