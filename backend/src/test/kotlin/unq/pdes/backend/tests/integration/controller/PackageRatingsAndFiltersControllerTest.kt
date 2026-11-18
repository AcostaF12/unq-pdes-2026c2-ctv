package unq.pdes.backend.tests.integration.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.service.FavoriteService

@SpringBootTest
@WithMockUser(username = "buyer", roles = ["BUYER"])
class PackageRatingsAndFiltersControllerTest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var favoriteService: FavoriteService

    private lateinit var mvc: MockMvc
    private lateinit var parisGold: TravelPackage
    private lateinit var parisLite: TravelPackage

    @BeforeEach
    fun setUp() {
        mvc =
            MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
        factory.buyerNamed("buyer")
        parisGold = factory.packageNamed("Paris Gold", agencyName = "Despegar", hotelName = "Hotel Sol")
        parisLite = factory.packageNamed("Paris Lite", agencyName = "Almundo", hotelName = "Hotel Luna")
        factory.packageNamed("Londres City", destinationCode = "LON", destinationCity = "London", agencyName = "Despegar")
        review("alice", parisGold, 10)
        review("bob", parisGold, 8)
        review("alice", parisLite, 6)
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - GET packages should expose the average score and review count`() {
        mvc
            .perform(get("/packages").param("name", "Gold"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].averageScore").value(9.0))
            .andExpect(jsonPath("$.content[0].reviewCount").value(2))
    }

    @Test
    fun `02 - GET packages should report no score for packages without reviews`() {
        mvc
            .perform(get("/packages").param("destination", "LON"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].averageScore").doesNotExist())
            .andExpect(jsonPath("$.content[0].reviewCount").value(0))
    }

    @Test
    fun `03 - GET packages should filter by minimum rating`() {
        mvc
            .perform(get("/packages").param("minRating", "7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Paris Gold"))
    }

    @Test
    fun `04 - GET packages with a minimum rating above 10 should return 400`() {
        mvc
            .perform(get("/packages").param("minRating", "11"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("The minimum rating must be between 0 and 10."))
    }

    @Test
    fun `05 - GET packages should filter by agency and by hotel`() {
        mvc
            .perform(get("/packages").param("agencyId", parisLite.agency.id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Paris Lite"))

        mvc
            .perform(get("/packages").param("hotel", "hotel sol"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Paris Gold"))
    }

    @Test
    fun `06 - GET packages should sort by rating and by destination name`() {
        mvc
            .perform(get("/packages").param("sort", "rating,desc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].name").value("Paris Gold"))
            .andExpect(jsonPath("$.content[1].name").value("Paris Lite"))
            .andExpect(jsonPath("$.content[2].name").value("Londres City"))

        mvc
            .perform(get("/packages").param("sort", "destination.name,asc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].name").value("Londres City"))
    }

    @Test
    fun `07 - GET packages should reject sorting by an unknown property`() {
        mvc
            .perform(get("/packages").param("sort", "agency.id,asc"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `08 - GET favorites should expose the average score and review count`() {
        favoriteService.add("buyer", parisGold.id!!)

        mvc
            .perform(get("/favorites"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].travelPackage.averageScore").value(9.0))
            .andExpect(jsonPath("$[0].travelPackage.reviewCount").value(2))
    }

    @Test
    @WithMockUser(username = "agency", roles = ["AGENCY"])
    fun `09 - GET packages agency should expose the average score and review count`() {
        factory.agencyUserNamed("agency")

        mvc
            .perform(get("/packages/agency"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.name=='Paris Gold')].averageScore").value(9.0))
            .andExpect(jsonPath("$[?(@.name=='Paris Gold')].reviewCount").value(2))
    }

    private fun review(
        username: String,
        travelPackage: TravelPackage,
        score: Int,
    ) {
        reviewRepository.save(
            Review
                .Builder()
                .buyer(factory.buyerNamed(username))
                .travelPackage(travelPackage)
                .score(score)
                .build(),
        )
    }
}
