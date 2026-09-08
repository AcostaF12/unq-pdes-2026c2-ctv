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
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.service.PurchaseService

@SpringBootTest
class PurchaseServiceTest {

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

        assertEquals(1, purchases.size)
        assertEquals("París Romántico", purchases.first().travelPackage.name)
    }

    @Test
    fun `05 - findForAgency should return sales of the agency`() {
        factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        val sales = purchaseService.findForAgency("agency")

        assertEquals(1, sales.size)
        assertEquals(buyer.username, sales.first().buyer.username)
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
