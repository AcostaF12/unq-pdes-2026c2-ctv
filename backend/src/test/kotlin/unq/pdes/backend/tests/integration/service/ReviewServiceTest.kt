package unq.pdes.backend.tests.integration.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.external.flights.ExternalFlightSaleDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.service.PurchaseService
import unq.pdes.backend.service.ReviewService
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
class ReviewServiceTest {
    @Autowired
    private lateinit var reviewService: ReviewService

    @Autowired
    private lateinit var purchaseService: PurchaseService

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @MockitoBean
    private lateinit var flightsClient: FlightsClient

    @BeforeEach
    fun setUp() {
        Mockito
            .`when`(flightsClient.sell(1L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(10L, 1L, "Bruno Buyer"))
        Mockito
            .`when`(flightsClient.sell(2L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(11L, 2L, "Bruno Buyer"))
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - cannot review a package that was not purchased`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                reviewService.create(buyer.username, travelPackage.id!!, 9, "Muy bueno")
            }

        assertEquals("You can only review packages you purchased.", exception.message)
    }

    @Test
    fun `02 - buyer can review a purchased package`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        val review = reviewService.create(buyer.username, travelPackage.id!!, 9, "Excelente")

        assertEquals(9, review.score)
        assertEquals(1, reviewService.findByPackageId(travelPackage.id!!).size)
    }

    @Test
    fun `03 - cannot review the same package twice`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)
        reviewService.create(buyer.username, travelPackage.id!!, 9, "Excelente")

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                reviewService.create(buyer.username, travelPackage.id!!, 8, "Otra")
            }

        assertEquals("You already reviewed this package.", exception.message)
    }

    @Test
    fun `04 - agency users cannot write reviews`() {
        factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("París Romántico")

        val exception =
            assertThrows(org.springframework.security.access.AccessDeniedException::class.java) {
                reviewService.create("agency", travelPackage.id!!, 9, "Excelente")
            }

        assertEquals("Only buyers can write reviews.", exception.message)
    }

    @Test
    fun `05 - a cancelled purchase does not allow reviewing but keeps existing reviews`() {
        val buyer = factory.buyerNamed("buyer")
        val reviewed = factory.packageNamed("París Romántico")
        val notReviewed = factory.packageNamed("Londres Clásico", destinationCode = "LON", destinationCity = "London")
        Mockito
            .`when`(flightsClient.findById(1L))
            .thenReturn(ExternalFlightDto(1L, "Demo", LocalDate.now().plusDays(10), LocalTime.NOON, "BUE", "PAR", 180, 100))
        val first = purchaseService.purchase(buyer.username, reviewed.id!!)
        val second = purchaseService.purchase(buyer.username, notReviewed.id!!)
        reviewService.create(buyer.username, reviewed.id!!, 9, "Excelente")

        purchaseService.cancel(buyer.username, first.id!!)
        purchaseService.cancel(buyer.username, second.id!!)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                reviewService.create(buyer.username, notReviewed.id!!, 8, "Otra")
            }
        assertEquals("You can only review packages you purchased.", exception.message)
        assertEquals(1, reviewService.findByPackageId(reviewed.id!!).size)
    }
}
