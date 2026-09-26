package unq.pdes.backend.tests.integration.service

import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import unq.pdes.backend.external.flights.ExternalFlightSaleDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.controller.dtos.models.PurchaseDto
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.persistence.jpa.TravelPackageRepository
import unq.pdes.backend.service.PurchaseService

@SpringBootTest
class PurchaseServiceTest {

    @Autowired
    private lateinit var purchaseService: PurchaseService

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @Autowired
    private lateinit var travelPackageRepository: TravelPackageRepository

    @MockitoBean
    private lateinit var flightsClient: FlightsClient

    @BeforeEach
    fun setUp() {
        Mockito.`when`(flightsClient.sell(1L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(10L, 1L, "Bruno Buyer"))
        Mockito.`when`(flightsClient.sell(2L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(11L, 2L, "Bruno Buyer"))
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - purchase should freeze price and agency and sell both flights`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")

        val purchase = purchaseService.purchase(buyer.username, travelPackage.id!!)

        assertNotNull(purchase.id)
        assertEquals(travelPackage.price, purchase.purchasePrice)
        assertEquals(travelPackage.agency.id, purchase.agency.id)
        Mockito.verify(flightsClient).sell(1L, "Bruno Buyer")
        Mockito.verify(flightsClient).sell(2L, "Bruno Buyer")
    }

    @Test
    fun `01a - history should keep the original trip after the package is updated`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)
        travelPackage.name = "París actualizado"
        travelPackage.price = java.math.BigDecimal("1800.00")
        travelPackageRepository.save(travelPackage)

        val history = purchaseService.findMine(buyer.username).content.single()
        val historyDto = PurchaseDto.fromModel(history)

        assertEquals("París Romántico", historyDto.travelPackage.name)
        assertEquals(java.math.BigDecimal("1500.00"), historyDto.travelPackage.price)
        assertEquals(java.math.BigDecimal("1500.00"), historyDto.purchasePrice)
    }

    @Test
    fun `02 - purchase should compensate outbound sale when return sale fails`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        Mockito.`when`(flightsClient.sell(2L, "Bruno Buyer"))
            .thenThrow(IllegalArgumentException("The flight has no availability."))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            purchaseService.purchase(buyer.username, travelPackage.id!!)
        }

        assertEquals("The flight has no availability.", exception.message)
        Mockito.verify(flightsClient).cancelSale(10L)
    }

    @Test
    fun `03 - agency users cannot purchase packages`() {
        factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("París Romántico")

        val exception = assertThrows(org.springframework.security.access.AccessDeniedException::class.java) {
            purchaseService.purchase("agency", travelPackage.id!!)
        }

        assertEquals("Only buyers can purchase packages.", exception.message)
    }

    @Test
    fun `04 - findMine should return purchases of the buyer`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        val purchases = purchaseService.findMine(buyer.username)

        assertEquals(1, purchases.totalElements)
        assertEquals("París Romántico", purchases.content.first().travelPackage.name)
    }

    @Test
    fun `04a - findMine should not include purchases from other buyers`() {
        val buyer = factory.buyerNamed("buyer")
        val otherBuyer = factory.buyerNamed("other-buyer")
        val firstPackage = factory.packageNamed("París Romántico")
        val secondPackage = factory.packageNamed("Londres Clásico", destinationCode = "LON", destinationCity = "London")
        purchaseService.purchase(buyer.username, firstPackage.id!!)
        purchaseService.purchase(otherBuyer.username, secondPackage.id!!)

        val purchases = purchaseService.findMine(buyer.username)

        assertEquals(listOf("París Romántico"), purchases.content.map { it.travelPackage.name })
    }

    @Test
    fun `04b - findMine should reject users that are not buyers`() {
        factory.agencyUserNamed("agency")

        val exception = assertThrows(org.springframework.security.access.AccessDeniedException::class.java) {
            purchaseService.findMine("agency")
        }

        assertEquals("Only buyers can list their purchase history.", exception.message)
    }

    @Test
    fun `04c - purchase history should paginate and report total pages`() {
        val buyer = factory.buyerNamed("buyer")
        val firstPackage = factory.packageNamed("París Romántico")
        val secondPackage = factory.packageNamed("Londres Clásico", destinationCode = "LON", destinationCity = "London")
        purchaseService.purchase(buyer.username, firstPackage.id!!)
        purchaseService.purchase(buyer.username, secondPackage.id!!)

        val firstPage = purchaseService.findMine(buyer.username, page = 0, size = 1)
        val secondPage = purchaseService.findMine(buyer.username, page = 1, size = 1)

        assertEquals(2, firstPage.totalElements)
        assertEquals(2, firstPage.totalPages)
        assertEquals(1, firstPage.content.size)
        assertEquals(0, firstPage.number)
        assertEquals(1, secondPage.number)
        assertEquals(1, secondPage.content.size)
        assertNotNull(firstPage.content.single().id)
        assertNotNull(secondPage.content.single().id)
        org.junit.jupiter.api.Assertions.assertNotEquals(firstPage.content.single().id, secondPage.content.single().id)
    }

    @Test
    fun `05 - findForAgency should return sales of the agency`() {
        factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        val sales = purchaseService.findForAgency("agency")

        assertEquals(1, sales.totalElements)
        assertEquals(buyer.username, sales.content.first().buyer.username)
    }

    @Test
    fun `05b - agency history should filter by buyer package and purchase date`() {
        factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        val otherBuyer = factory.buyerNamed("other-buyer")
        val matchingPackage = factory.packageNamed("París Romántico")
        val otherPackage = factory.packageNamed("Londres Clásico", destinationCode = "LON", destinationCity = "London")
        purchaseService.purchase(buyer.username, matchingPackage.id!!)
        purchaseService.purchase(otherBuyer.username, otherPackage.id!!)
        val today = LocalDate.now()

        val results = purchaseService.findForAgency(
            username = "agency",
            page = 0,
            size = 5,
            buyerUsername = buyer.username,
            packageName = "románt",
            from = today,
            to = today,
        )

        assertEquals(1, results.totalElements)
        assertEquals(buyer.username, results.content.single().buyer.username)
        assertEquals("París Romántico", results.content.single().travelPackage.name)
    }

    @Test
    fun `05c - history rejects invalid pagination and inverted date range`() {
        val buyer = factory.buyerNamed("buyer")
        factory.agencyUserNamed("agency")

        assertThrows(IllegalArgumentException::class.java) { purchaseService.findMine(buyer.username, page = -1) }
        assertThrows(IllegalArgumentException::class.java) { purchaseService.findMine(buyer.username, size = 101) }
        assertThrows(IllegalArgumentException::class.java) {
            purchaseService.findForAgency("agency", from = LocalDate.now(), to = LocalDate.now().minusDays(1))
        }
    }

    @Test
    fun `05a - findForAgency should not include sales from other agencies`() {
        factory.agencyUserNamed("agency")
        factory.agencyUserNamed("other-agency", agencyName = "Otra agencia")
        val buyer = factory.buyerNamed("buyer")
        val otherBuyer = factory.buyerNamed("other-buyer")
        val agencyPackage = factory.packageNamed("París Romántico")
        val otherAgencyPackage = factory.packageNamed(
            "Londres Clásico",
            destinationCode = "LON",
            destinationCity = "London",
            agencyName = "Otra agencia",
        )
        purchaseService.purchase(buyer.username, agencyPackage.id!!)
        purchaseService.purchase(otherBuyer.username, otherAgencyPackage.id!!)

        val sales = purchaseService.findForAgency("agency")

        assertEquals(listOf("París Romántico"), sales.content.map { it.travelPackage.name })
    }

    @Test
    fun `06 - buyers cannot list agency purchases`() {
        val buyer = factory.buyerNamed("buyer")

        val exception = assertThrows(org.springframework.security.access.AccessDeniedException::class.java) {
            purchaseService.findForAgency(buyer.username)
        }

        assertEquals("Only agency users can list agency purchases.", exception.message)
    }
}
