package unq.pdes.backend.tests.integration.controller

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.external.flights.ExternalFlightSaleDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.external.flights.FlightsServiceUnavailableException
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.PurchaseStatus
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.service.PurchaseService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest
class PurchaseCancellationControllerTest {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var factory: PersistentObjectsFactory

    @Autowired private lateinit var purchaseService: PurchaseService

    @Autowired private lateinit var purchases: PurchaseRepository

    @Autowired private lateinit var cleanup: DataServiceH2

    @MockitoBean private lateinit var flightsClient: FlightsClient

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
        Mockito
            .`when`(flightsClient.sell(1L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(10L, 1L, "Bruno Buyer"))
        Mockito
            .`when`(flightsClient.sell(2L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(11L, 2L, "Bruno Buyer"))
        outboundDepartsOn(LocalDate.now().plusDays(15))
    }

    @AfterEach
    fun tearDown() = cleanup.deleteAll()

    private fun outboundDepartsOn(date: LocalDate) {
        Mockito
            .`when`(flightsClient.findById(1L))
            .thenReturn(ExternalFlightDto(1L, "Demo", date, LocalTime.of(8, 0), "BUE", "PAR", 180, 100))
    }

    private fun boughtByBuyer(): Purchase {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        return purchaseService.purchase(buyer.username, travelPackage.id!!)
    }

    private fun reload(purchase: Purchase): Purchase = purchases.findById(purchase.id!!).orElseThrow()

    @Test
    fun `01 - purchase stores the sale ids of both flights`() {
        val purchase = reload(boughtByBuyer())

        assertEquals(10L, purchase.outboundSaleId)
        assertEquals(11L, purchase.returnSaleId)
        assertEquals(PurchaseStatus.ACTIVE, purchase.status)
    }

    @Test
    fun `02 - the buyer cancels the purchase, both seats are released and it is kept as cancelled`() {
        val purchase = boughtByBuyer()

        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("buyer").roles("BUYER")))
            .andExpect(status().isNoContent)

        Mockito.verify(flightsClient).cancelSale(10L)
        Mockito.verify(flightsClient).cancelSale(11L)
        val cancelled = reload(purchase)
        assertEquals(PurchaseStatus.CANCELLED, cancelled.status)
        assertNotNull(cancelled.cancelledAt)
        mvc
            .perform(get("/purchases/me").with(user("buyer").roles("BUYER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("CANCELLED"))
            .andExpect(jsonPath("$.content[0].cancelledAt").isNotEmpty)
        mvc
            .perform(get("/purchases/${purchase.id}").with(user("buyer").roles("BUYER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))
    }

    @Test
    fun `03 - an admin can cancel any purchase`() {
        factory.adminNamed("admin")
        val purchase = boughtByBuyer()

        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("admin").roles("ADMIN")))
            .andExpect(status().isNoContent)

        assertEquals(PurchaseStatus.CANCELLED, reload(purchase).status)
        mvc
            .perform(get("/admin/sales").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].status").value("CANCELLED"))
    }

    @Test
    fun `04 - another buyer gets 403 and the purchase stays active`() {
        val purchase = boughtByBuyer()
        factory.buyerNamed("intruder")

        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("intruder").roles("BUYER")))
            .andExpect(status().isForbidden)

        assertEquals(PurchaseStatus.ACTIVE, reload(purchase).status)
        Mockito.verify(flightsClient, Mockito.never()).cancelSale(Mockito.anyLong())
    }

    @Test
    fun `05 - agency users get 403 even for their own sales`() {
        factory.agencyUserNamed("agency")
        val purchase = boughtByBuyer()

        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("agency").roles("AGENCY")))
            .andExpect(status().isForbidden)

        assertEquals(PurchaseStatus.ACTIVE, reload(purchase).status)
    }

    @Test
    fun `06 - an unknown purchase returns 404`() {
        factory.buyerNamed("buyer")

        mvc
            .perform(delete("/purchases/999999").with(user("buyer").roles("BUYER")))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `07 - cancelling twice returns 400 and seats are released only once`() {
        val purchase = boughtByBuyer()
        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("buyer").roles("BUYER")))
            .andExpect(status().isNoContent)

        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("buyer").roles("BUYER")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorData.description").value("La compra ya fue cancelada."))

        Mockito.verify(flightsClient, Mockito.times(1)).cancelSale(10L)
    }

    @Test
    fun `08 - a purchase whose outbound flight already departed cannot be cancelled`() {
        val purchase = boughtByBuyer()
        outboundDepartsOn(LocalDate.now().minusDays(1))

        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("buyer").roles("BUYER")))
            .andExpect(status().isBadRequest)

        assertEquals(PurchaseStatus.ACTIVE, reload(purchase).status)
        Mockito.verify(flightsClient, Mockito.never()).cancelSale(Mockito.anyLong())
    }

    @Test
    fun `09 - flights-service unavailable returns 503 and keeps the purchase active`() {
        val purchase = boughtByBuyer()
        Mockito
            .`when`(flightsClient.findById(1L))
            .thenThrow(FlightsServiceUnavailableException("Flights service is unavailable."))

        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("buyer").roles("BUYER")))
            .andExpect(status().isServiceUnavailable)

        assertEquals(PurchaseStatus.ACTIVE, reload(purchase).status)
    }

    @Test
    fun `10 - a failure releasing the return seat keeps the purchase active and a retry completes it`() {
        val purchase = boughtByBuyer()
        Mockito
            .doThrow(FlightsServiceUnavailableException("Flights service is unavailable."))
            .`when`(flightsClient)
            .cancelSale(11L)

        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("buyer").roles("BUYER")))
            .andExpect(status().isServiceUnavailable)
        assertEquals(PurchaseStatus.ACTIVE, reload(purchase).status)

        // Retry: the outbound sale was already released (404) and counts as released.
        Mockito.reset(flightsClient)
        outboundDepartsOn(LocalDate.now().plusDays(15))
        Mockito
            .doThrow(EntityNotFoundException("There is no FlightSale with id: 10."))
            .`when`(flightsClient)
            .cancelSale(10L)

        mvc
            .perform(delete("/purchases/${purchase.id}").with(user("buyer").roles("BUYER")))
            .andExpect(status().isNoContent)
        Mockito.verify(flightsClient).cancelSale(11L)
        assertEquals(PurchaseStatus.CANCELLED, reload(purchase).status)
    }

    @Test
    fun `11 - legacy purchases without sale ids are cancelled locally only`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        val legacy =
            purchases.save(
                Purchase
                    .Builder()
                    .buyer(buyer)
                    .travelPackage(travelPackage)
                    .agency(travelPackage.agency)
                    .purchasePrice(travelPackage.price)
                    .purchasedAt(LocalDateTime.now())
                    .build(),
            )

        mvc
            .perform(delete("/purchases/${legacy.id}").with(user("buyer").roles("BUYER")))
            .andExpect(status().isNoContent)

        Mockito.verify(flightsClient, Mockito.never()).cancelSale(Mockito.anyLong())
        assertEquals(PurchaseStatus.CANCELLED, reload(legacy).status)
    }

    @Test
    fun `12 - already bought only considers active purchases`() {
        val purchase = boughtByBuyer()
        val packageId = purchase.travelPackage.id!!
        mvc
            .perform(get("/purchases/me/packages/$packageId").with(user("buyer").roles("BUYER")))
            .andExpect(content().string("true"))

        purchaseService.cancel("buyer", purchase.id!!)

        mvc
            .perform(get("/purchases/me/packages/$packageId").with(user("buyer").roles("BUYER")))
            .andExpect(status().isOk)
            .andExpect(content().string("false"))
    }

    @Test
    fun `13 - the agency history still lists cancelled sales with their status`() {
        factory.agencyUserNamed("agency")
        val purchase = boughtByBuyer()
        purchaseService.cancel("buyer", purchase.id!!)

        mvc
            .perform(get("/purchases/agency").with(user("agency").roles("AGENCY")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("CANCELLED"))
    }

    @Test
    fun `14 - anonymous users cannot cancel purchases`() {
        val purchase = boughtByBuyer()

        mvc
            .perform(delete("/purchases/${purchase.id}"))
            .andExpect(status().isForbidden)
    }
}
