package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import unq.pdes.backend.controller.dtos.requests.FavoriteRequestDto
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2

@SpringBootTest
@WithMockUser(username = "buyer", roles = ["BUYER"])
class FavoriteControllerTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

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

    @Test
    fun `01 - GET favorites should return the buyer favorites`() {
        factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        mvc.perform(
            post("/favorites").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(FavoriteRequestDto(travelPackage.id!!))),
        )

        mvc.perform(get("/favorites"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].travelPackage.name").value("París Romántico"))
    }

    @Test
    fun `02 - POST favorites should create a favorite`() {
        factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")

        mvc.perform(
            post("/favorites").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(FavoriteRequestDto(travelPackage.id!!))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.packageId").value(travelPackage.id))
            .andExpect(jsonPath("$.travelPackage.name").value("París Romántico"))
    }

    @Test
    fun `03 - DELETE favorites should remove the favorite`() {
        factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        mvc.perform(
            post("/favorites").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(FavoriteRequestDto(travelPackage.id!!))),
        )

        mvc.perform(delete("/favorites/{packageId}", travelPackage.id))
            .andExpect(status().isNoContent)

        mvc.perform(get("/favorites"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `04 - POST favorites as agency should return 403`() {
        factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("París Romántico")

        mvc.perform(
            post("/favorites").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(FavoriteRequestDto(travelPackage.id!!))),
        )
            .andExpect(status().isForbidden)
    }
}
