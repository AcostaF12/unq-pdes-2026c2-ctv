package unq.pdes.backend.tests.integration.controller

import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import unq.pdes.backend.controller.dtos.requests.HotelRequestDto
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2

@SpringBootTest
@WithMockUser(roles = ["ADMIN"])
class HotelControllerTest {

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

    private fun json(request: HotelRequestDto): String = objectMapper.writeValueAsString(request)

    @Test
    fun `01 - GET hotels should return the list of hotels`() {
        factory.hotelNamed("The Savoy")

        mvc.perform(get("/hotels"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("The Savoy"))
    }

    @Test
    fun `02 - GET hotel by id should return the hotel`() {
        val hotel = factory.hotelNamed("The Savoy")

        mvc.perform(get("/hotels/{id}", hotel.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(hotel.id))
            .andExpect(jsonPath("$.name").value("The Savoy"))
            .andExpect(jsonPath("$.city.code").value("BUE"))
    }

    @Test
    fun `03 - GET hotel by id should return 404 when not found`() {
        mvc.perform(get("/hotels/{id}", 999))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.httpCode").value(404))
            .andExpect(jsonPath("$.errorData.description").value("There is no Hotel with id: 999."))
    }

    @Test
    fun `04 - POST hotels should create the hotel and return 201`() {
        factory.cityWith("PAR", "Paris")
        val request = HotelRequestDto("Hotel Le Meurice", "PAR", "https://x.demo/lm.jpg")

        mvc.perform(post("/hotels").contentType(MediaType.APPLICATION_JSON).content(json(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.name").value("Hotel Le Meurice"))
            .andExpect(jsonPath("$.city.code").value("PAR"))
    }

    @Test
    fun `05 - POST hotels with blank name should return 400`() {
        factory.cityWith("PAR", "Paris")
        val request = HotelRequestDto("  ", "PAR", "https://x.demo/lm.jpg")

        mvc.perform(post("/hotels").contentType(MediaType.APPLICATION_JSON).content(json(request)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.httpCode").value(400))
            .andExpect(jsonPath("$.errorData.description").value("The hotel must have a name."))
    }

    @Test
    fun `06 - POST hotels with unknown city should return 404`() {
        val request = HotelRequestDto("Some hotel", "ZZZ", "https://x.demo/p.jpg")

        mvc.perform(post("/hotels").contentType(MediaType.APPLICATION_JSON).content(json(request)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorData.description").value("There is no City with code: ZZZ."))
    }

    @Test
    fun `07 - PUT hotels should update the hotel`() {
        val hotel = factory.hotelNamed("Old name")
        val request = HotelRequestDto("New name", "BUE", "https://x.demo/new.jpg")

        mvc.perform(put("/hotels/{id}", hotel.id).contentType(MediaType.APPLICATION_JSON).content(json(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(hotel.id))
            .andExpect(jsonPath("$.name").value("New name"))
    }

    @Test
    fun `08 - DELETE hotels should remove the hotel and return 204`() {
        val hotel = factory.hotelNamed("The Savoy")

        mvc.perform(delete("/hotels/{id}", hotel.id))
            .andExpect(status().isNoContent)

        mvc.perform(get("/hotels/{id}", hotel.id))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["BUYER"])
    fun `09 - POST hotels as a non-admin should return 403`() {
        factory.cityWith("PAR", "Paris")
        val request = HotelRequestDto("Hotel Le Meurice", "PAR", "https://x.demo/lm.jpg")

        mvc.perform(post("/hotels").contentType(MediaType.APPLICATION_JSON).content(json(request)))
            .andExpect(status().isForbidden)
    }
}
