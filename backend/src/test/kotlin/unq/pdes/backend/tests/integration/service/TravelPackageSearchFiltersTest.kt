package unq.pdes.backend.tests.integration.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.search.TravelPackageSearchCriteria
import unq.pdes.backend.service.ReviewService
import unq.pdes.backend.service.TravelPackageService
import java.math.BigDecimal

@SpringBootTest
class TravelPackageSearchFiltersTest {
    @Autowired
    private lateinit var travelPackageService: TravelPackageService

    @Autowired
    private lateinit var reviewService: ReviewService

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    private lateinit var parisGold: TravelPackage
    private lateinit var parisLite: TravelPackage
    private lateinit var londonCity: TravelPackage

    @BeforeEach
    fun setUp() {
        parisGold = factory.packageNamed("Paris Gold", agencyName = "Despegar", hotelName = "Hotel Sol")
        parisLite = factory.packageNamed("Paris Lite", agencyName = "Almundo", hotelName = "Hotel Luna")
        londonCity =
            factory.packageNamed(
                "Londres City",
                destinationCode = "LON",
                destinationCity = "London",
                agencyName = "Despegar",
                hotelName = "Sol Londres",
            )
        review("alice", parisGold, 10)
        review("bob", parisGold, 8)
        review("alice", parisLite, 6)
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - search should filter by agency`() {
        val agencyId = parisLite.agency.id!!

        val result = search(TravelPackageSearchCriteria.from(null, null, null, null, null, agencyId = agencyId))

        assertEquals(listOf("Paris Lite"), result)
    }

    @Test
    fun `02 - search should filter by hotel name ignoring case`() {
        val result = search(TravelPackageSearchCriteria.from(null, null, null, null, null, hotel = "  SOL "))

        assertEquals(listOf("Londres City", "Paris Gold"), result)
    }

    @Test
    fun `03 - search should keep only packages whose average score reaches the minimum rating`() {
        val result = search(TravelPackageSearchCriteria.from(null, null, null, null, null, minRating = BigDecimal("7")))

        assertEquals(listOf("Paris Gold"), result)
    }

    @Test
    fun `04 - search with a minimum rating of zero should still exclude packages without reviews`() {
        val result = search(TravelPackageSearchCriteria.from(null, null, null, null, null, minRating = BigDecimal.ZERO))

        assertEquals(listOf("Paris Gold", "Paris Lite"), result)
    }

    @Test
    fun `05 - search should sort by rating descending leaving unreviewed packages last`() {
        val result = search(TravelPackageSearchCriteria(), Sort.by(Sort.Order.desc("rating")))

        assertEquals(listOf("Paris Gold", "Paris Lite", "Londres City"), result)
    }

    @Test
    fun `06 - search should sort by rating ascending leaving unreviewed packages first`() {
        val result = search(TravelPackageSearchCriteria(), Sort.by(Sort.Order.asc("rating")))

        assertEquals(listOf("Londres City", "Paris Lite", "Paris Gold"), result)
    }

    @Test
    fun `07 - search should paginate and count correctly when sorting by rating`() {
        val page = travelPackageService.search(TravelPackageSearchCriteria(), PageRequest.of(1, 2, Sort.by(Sort.Order.desc("rating"))))

        assertEquals(3, page.totalElements)
        assertEquals(2, page.totalPages)
        assertEquals(listOf("Londres City"), page.content.map { it.name })
    }

    @Test
    fun `08 - search should combine rating sort with other filters`() {
        val criteria = TravelPackageSearchCriteria.from(null, null, "PAR", null, null)

        val result = search(criteria, Sort.by(Sort.Order.asc("rating")))

        assertEquals(listOf("Paris Lite", "Paris Gold"), result)
    }

    @Test
    fun `09 - search should sort by destination name`() {
        val result = search(TravelPackageSearchCriteria(), Sort.by("destination.name", "name"))

        assertEquals(listOf("Londres City", "Paris Gold", "Paris Lite"), result)
    }

    @Test
    fun `10 - search should reject a minimum rating outside the 0 to 10 range`() {
        val tooHigh =
            assertThrows(IllegalArgumentException::class.java) {
                TravelPackageSearchCriteria.from(null, null, null, null, null, minRating = BigDecimal("10.5"))
            }
        val negative =
            assertThrows(IllegalArgumentException::class.java) {
                TravelPackageSearchCriteria.from(null, null, null, null, null, minRating = BigDecimal("-1"))
            }

        assertEquals("The minimum rating must be between 0 and 10.", tooHigh.message)
        assertEquals("The minimum rating must be between 0 and 10.", negative.message)
    }

    @Test
    fun `11 - summarize should return the average and count per package in one pass`() {
        val summaries = reviewService.summarize(listOf(parisGold.id!!, parisLite.id!!, londonCity.id!!))

        assertEquals(9.0, summaries.getValue(parisGold.id!!).averageScore)
        assertEquals(2, summaries.getValue(parisGold.id!!).reviewCount)
        assertEquals(6.0, summaries.getValue(parisLite.id!!).averageScore)
        assertEquals(false, summaries.containsKey(londonCity.id!!))
    }

    @Test
    fun `12 - summarize should return nothing for an empty list of packages`() {
        assertEquals(emptyMap<Long, Any>(), reviewService.summarize(emptyList()))
    }

    @Test
    fun `13 - findByBuyerAndPackage should return the review of that buyer only`() {
        val alice = factory.buyerNamed("alice")
        val carol = factory.buyerNamed("carol")

        assertEquals(10, reviewService.findByBuyerAndPackage(alice.id!!, parisGold.id!!)?.score)
        assertEquals(null, reviewService.findByBuyerAndPackage(carol.id!!, parisGold.id!!))
    }

    private fun search(
        criteria: TravelPackageSearchCriteria,
        sort: Sort = Sort.by("name"),
    ): List<String> = travelPackageService.search(criteria, PageRequest.of(0, 20, sort)).content.map { it.name }

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
