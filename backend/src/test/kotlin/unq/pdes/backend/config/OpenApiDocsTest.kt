package unq.pdes.backend.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertContains

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `api-docs endpoint should expose the OpenAPI spec`() {
        val body =
            mockMvc
                .get("/v3/api-docs")
                .andExpect { status { isOk() } }
                .andReturn()
                .response.contentAsString

        assertContains(body, "\"openapi\"")
        assertContains(body, "CTV - Backend API")
    }

    @Test
    fun `swagger-ui should redirect to the index page`() {
        mockMvc
            .get("/swagger-ui.html")
            .andExpect { status { is3xxRedirection() } }
    }
}
