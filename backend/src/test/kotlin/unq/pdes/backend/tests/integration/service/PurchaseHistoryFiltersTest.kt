package unq.pdes.backend.tests.integration.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.access.AccessDeniedException
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.service.PurchaseService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class PurchaseHistoryFiltersTest {
    @Autowired
    private lateinit var purchaseService: PurchaseService

    @Autowired
    private lateinit var purchaseRepository: PurchaseRepository

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    private val buyerName = "buyer"

    @BeforeEach
    fun setUp() {
        val paris = factory.packageNamed("París Romántico", price = BigDecimal("1500.00"))
        val london =
            factory.packageNamed("Londres Clásico", destinationCode = "LON", destinationCity = "London", price = BigDecimal("1800.00"))
        val rome =
            factory.packageNamed("Roma Imperial", destinationCode = "ROM", destinationCity = "Rome", price = BigDecimal("1000.00"))
        purchase(buyerName, paris, LocalDateTime.of(2026, 3, 10, 23, 30))
        purchase(buyerName, london, LocalDateTime.of(2026, 4, 1, 0, 0))
        purchase(buyerName, rome, LocalDateTime.of(2026, 5, 20, 12, 0))
        purchase("other", paris, LocalDateTime.of(2026, 4, 2, 12, 0))
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - findMine should return only the buyer purchases newest first by default`() {
        assertEquals(listOf("Roma Imperial", "Londres Clásico", "París Romántico"), names())
    }

    @Test
    fun `02 - findMine should filter by package name ignoring case`() {
        assertEquals(listOf("París Romántico"), names(packageName = "  románt "))
    }

    @Test
    fun `03 - findMine should filter by destination code`() {
        assertEquals(listOf("Londres Clásico"), names(destination = "lon"))
    }

    @Test
    fun `04 - findMine should include both ends of the date range`() {
        val march10 = LocalDate.of(2026, 3, 10)
        val april1 = LocalDate.of(2026, 4, 1)

        assertEquals(listOf("Londres Clásico", "París Romántico"), names(from = march10, to = april1))
        assertEquals(listOf("París Romántico"), names(from = march10, to = march10))
    }

    @Test
    fun `05 - findMine should filter by an inclusive price range`() {
        assertEquals(
            listOf("Londres Clásico", "París Romántico"),
            names(minPrice = BigDecimal("1500.00"), maxPrice = BigDecimal("1800.00")),
        )
    }

    @Test
    fun `06 - findMine should sort by date and by price in both directions`() {
        assertEquals(listOf("París Romántico", "Londres Clásico", "Roma Imperial"), names(sort = "date,asc"))
        assertEquals(listOf("Roma Imperial", "París Romántico", "Londres Clásico"), names(sort = "price,asc"))
        assertEquals(listOf("Londres Clásico", "París Romántico", "Roma Imperial"), names(sort = "price,desc"))
    }

    @Test
    fun `07 - findMine should combine filters with sorting and paging`() {
        val page =
            purchaseService.findMine(buyerName, page = 1, size = 1, minPrice = BigDecimal("1000.00"), sort = "price,asc")

        assertEquals(3, page.totalElements)
        assertEquals(listOf("París Romántico"), page.content.map { it.travelSnapshot.name })
    }

    @Test
    fun `08 - findMine should reject an unknown sort`() {
        val exception = assertThrows(IllegalArgumentException::class.java) { names(sort = "name,asc") }

        assertEquals("Cannot sort purchases by 'name,asc'.", exception.message)
    }

    @Test
    fun `09 - findMine should reject an inverted date range and an inverted price range`() {
        val dates =
            assertThrows(IllegalArgumentException::class.java) {
                names(from = LocalDate.of(2026, 5, 1), to = LocalDate.of(2026, 4, 1))
            }
        val prices =
            assertThrows(IllegalArgumentException::class.java) {
                names(minPrice = BigDecimal("2000"), maxPrice = BigDecimal("1000"))
            }

        assertEquals("The start date must not be after the end date.", dates.message)
        assertEquals("The minimum price cannot be greater than the maximum price.", prices.message)
    }

    @Test
    fun `10 - findForViewer should allow the buyer and the selling agency`() {
        val purchase = purchaseRepository.findAll().first { it.buyerSnapshot.username == buyerName }
        factory.agencyUserNamed("agency")

        assertEquals(purchase.id, purchaseService.findForViewer(buyerName, purchase.id!!).id)
        assertEquals(purchase.id, purchaseService.findForViewer("agency", purchase.id!!).id)
    }

    @Test
    fun `11 - findForViewer should deny other buyers other agencies and admins`() {
        val purchase = purchaseRepository.findAll().first { it.buyerSnapshot.username == buyerName }
        factory.agencyUserNamed("rival", "Almundo")
        factory.adminNamed("admin")

        listOf("other", "rival", "admin").forEach { username ->
            val exception =
                assertThrows(AccessDeniedException::class.java) { purchaseService.findForViewer(username, purchase.id!!) }
            assertEquals("You cannot access this purchase.", exception.message)
        }
    }

    @Test
    fun `12 - findForViewer should fail when the purchase does not exist`() {
        val exception =
            assertThrows(jakarta.persistence.EntityNotFoundException::class.java) {
                purchaseService.findForViewer(buyerName, 9999L)
            }

        assertEquals("There is no Purchase with id: 9999.", exception.message)
    }

    private fun names(
        packageName: String? = null,
        destination: String? = null,
        from: LocalDate? = null,
        to: LocalDate? = null,
        minPrice: BigDecimal? = null,
        maxPrice: BigDecimal? = null,
        sort: String? = null,
    ): List<String> =
        purchaseService
            .findMine(buyerName, 0, 20, packageName, destination, from, to, minPrice, maxPrice, sort)
            .content
            .map { it.travelSnapshot.name }

    private fun purchase(
        username: String,
        travelPackage: TravelPackage,
        purchasedAt: LocalDateTime,
    ) {
        purchaseRepository.save(
            Purchase
                .Builder()
                .buyer(factory.buyerNamed(username))
                .travelPackage(travelPackage)
                .agency(travelPackage.agency)
                .purchasePrice(travelPackage.price)
                .purchasedAt(purchasedAt)
                .build(),
        )
    }
}
