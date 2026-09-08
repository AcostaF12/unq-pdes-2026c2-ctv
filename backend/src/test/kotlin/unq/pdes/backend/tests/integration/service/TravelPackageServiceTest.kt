package unq.pdes.backend.tests.integration.service

import jakarta.persistence.EntityNotFoundException
import java.math.BigDecimal
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
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.bean.override.mockito.MockitoBean
import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.external.flights.ExternalFlightSaleDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.Review
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.service.FavoriteService
import unq.pdes.backend.service.PurchaseService
import unq.pdes.backend.service.TravelPackageService

@SpringBootTest
class TravelPackageServiceTest {

    @Autowired
    private lateinit var travelPackageService: TravelPackageService

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @Autowired
    private lateinit var purchaseService: PurchaseService

    @Autowired
    private lateinit var favoriteService: FavoriteService

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @MockitoBean
    private lateinit var flightsClient: FlightsClient

    @BeforeEach
    fun setUp() {
        stubRoundTrip(1L, 2L, "BUE", "PAR")
    }

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - create should persist a package with origin and destination from flights`() {
        factory.cityWith("BUE", "Buenos Aires")
        val hotel = factory.hotelIn(factory.cityWith("PAR", "Paris"))
        factory.agencyUserNamed("agency")

        val travelPackage = travelPackageService.create(
            "agency", "París Romántico", hotel.id!!, 1L, 2L, BigDecimal("1500.00"),
        )

        assertNotNull(travelPackage.id)
        assertEquals("BUE", travelPackage.origin.code)
        assertEquals("PAR", travelPackage.destination.code)
        assertEquals("París Romántico", travelPackage.name)
    }

    @Test
    fun `02 - create should reject a return flight that is not the inverse route`() {
        stubRoundTrip(1L, 2L, "BUE", "PAR", returnOrigin = "LON", returnDestination = "BUE")
        factory.cityWith("BUE", "Buenos Aires")
        val hotel = factory.hotelIn(factory.cityWith("PAR", "Paris"))
        factory.agencyUserNamed("agency")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            travelPackageService.create("agency", "París Romántico", hotel.id!!, 1L, 2L, BigDecimal("1500.00"))
        }

        assertEquals("The return flight origin must match the outbound destination.", exception.message)
    }

    @Test
    fun `03 - create should reject a hotel outside the destination city`() {
        factory.cityWith("BUE", "Buenos Aires")
        factory.cityWith("PAR", "Paris")
        val hotel = factory.hotelNamed("Hotel en Buenos Aires")
        factory.agencyUserNamed("agency")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            travelPackageService.create("agency", "París Romántico", hotel.id!!, 1L, 2L, BigDecimal("1500.00"))
        }

        assertEquals("The hotel must be located in the destination city.", exception.message)
    }

    @Test
    fun `04 - search should filter by destination`() {
        factory.packageNamed("París Romántico", destinationCode = "PAR", destinationCity = "Paris")
        factory.packageNamed("Londres Clásico", destinationCode = "LON", destinationCity = "London")

        val result = travelPackageService.search(null, null, "LON")

        assertEquals(1, result.size)
        assertEquals("Londres Clásico", result.first().name)
    }

    @Test
    fun `05 - findById should throw when the package does not exist`() {
        val exception = assertThrows(EntityNotFoundException::class.java) {
            travelPackageService.findById(999L)
        }

        assertEquals("There is no TravelPackage with id: 999.", exception.message)
    }

    @Test
    fun `06 - another agency should not update a package`() {
        factory.agencyUserNamed("agency")
        factory.agencyUserNamed("other", "Almundo")
        val travelPackage = factory.packageNamed("París Romántico")
        val hotelId = travelPackage.hotel.id!!

        val exception = assertThrows(AccessDeniedException::class.java) {
            travelPackageService.update(
                "other", travelPackage.id!!, "Nuevo", hotelId, 1L, 2L, BigDecimal("1600.00"),
            )
        }

        assertEquals("Only the owning agency can modify this package.", exception.message)
    }

    @Test
    fun `07 - create should reject a return destination that is not the inverse route`() {
        stubRoundTrip(1L, 2L, "BUE", "PAR", returnOrigin = "PAR", returnDestination = "MAD")
        factory.cityWith("BUE", "Buenos Aires")
        val hotel = factory.hotelIn(factory.cityWith("PAR", "Paris"))
        factory.agencyUserNamed("agency")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            travelPackageService.create("agency", "París Romántico", hotel.id!!, 1L, 2L, BigDecimal("1500.00"))
        }

        assertEquals("The return flight destination must match the outbound origin.", exception.message)
    }

    @Test
    fun `08 - create should reject a duplicated package name for the same agency`() {
        factory.cityWith("BUE", "Buenos Aires")
        val hotel = factory.hotelIn(factory.cityWith("PAR", "Paris"))
        factory.agencyUserNamed("agency")
        travelPackageService.create("agency", "París Romántico", hotel.id!!, 1L, 2L, BigDecimal("1500.00"))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            travelPackageService.create("agency", "París Romántico", hotel.id!!, 1L, 2L, BigDecimal("1600.00"))
        }

        assertEquals("The agency already has a package named 'París Romántico'.", exception.message)
    }

    @Test
    fun `09 - buyers cannot manage packages`() {
        factory.buyerNamed("buyer")
        factory.cityWith("BUE", "Buenos Aires")
        val hotel = factory.hotelIn(factory.cityWith("PAR", "Paris"))

        val exception = assertThrows(AccessDeniedException::class.java) {
            travelPackageService.create("buyer", "París Romántico", hotel.id!!, 1L, 2L, BigDecimal("1500.00"))
        }

        assertEquals("Only agency users can manage packages.", exception.message)
    }

    @Test
    fun `10 - findMine should return packages of the agency`() {
        factory.agencyUserNamed("agency")
        factory.packageNamed("París Romántico")

        val result = travelPackageService.findMine("agency")

        assertEquals(1, result.size)
        assertEquals("París Romántico", result.first().name)
    }

    @Test
    fun `11 - search should treat blank filters as absent`() {
        factory.packageNamed("París Romántico")

        val result = travelPackageService.search("  ", "  ", "  ")

        assertEquals(1, result.size)
    }

    @Test
    fun `12 - update should persist the new name and keep uniqueness per agency`() {
        factory.agencyUserNamed("agency")
        factory.cityWith("BUE", "Buenos Aires")
        val hotel = factory.hotelIn(factory.cityWith("PAR", "Paris"))
        val first = travelPackageService.create("agency", "París Romántico", hotel.id!!, 1L, 2L, BigDecimal("1500.00"))
        val second = travelPackageService.create("agency", "París Express", hotel.id!!, 1L, 2L, BigDecimal("1200.00"))

        val updated = travelPackageService.update(
            "agency", first.id!!, "París Premium", hotel.id!!, 1L, 2L, BigDecimal("1800.00"),
        )

        assertEquals("París Premium", updated.name)
        assertEquals(BigDecimal("1800.00"), updated.price)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            travelPackageService.update(
                "agency", second.id!!, "París Premium", hotel.id!!, 1L, 2L, BigDecimal("1200.00"),
            )
        }
        assertEquals("The agency already has a package named 'París Premium'.", exception.message)
    }

    @Test
    fun `13 - delete should remove a package without related records`() {
        factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("París Romántico")

        travelPackageService.deleteById("agency", travelPackage.id!!)

        val exception = assertThrows(EntityNotFoundException::class.java) {
            travelPackageService.findById(travelPackage.id!!)
        }
        assertEquals("There is no TravelPackage with id: ${travelPackage.id}.", exception.message)
    }

    @Test
    fun `14 - delete should reject a package with purchases`() {
        stubSales()
        factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        purchaseService.purchase(buyer.username, travelPackage.id!!)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            travelPackageService.deleteById("agency", travelPackage.id!!)
        }

        assertEquals("Cannot delete a package that already has purchases.", exception.message)
    }

    @Test
    fun `15 - delete should reject a package marked as favorite`() {
        factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        favoriteService.add(buyer.username, travelPackage.id!!)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            travelPackageService.deleteById("agency", travelPackage.id!!)
        }

        assertEquals("Cannot delete a package that is marked as favorite.", exception.message)
    }

    @Test
    fun `16 - delete should reject a package with reviews`() {
        factory.agencyUserNamed("agency")
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        reviewRepository.save(
            Review.Builder()
                .buyer(buyer)
                .travelPackage(travelPackage)
                .score(9)
                .comment("Excelente")
                .build(),
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            travelPackageService.deleteById("agency", travelPackage.id!!)
        }

        assertEquals("Cannot delete a package that already has reviews.", exception.message)
    }

    private fun stubSales() {
        Mockito.`when`(flightsClient.sell(1L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(10L, 1L, "Bruno Buyer"))
        Mockito.`when`(flightsClient.sell(2L, "Bruno Buyer"))
            .thenReturn(ExternalFlightSaleDto(11L, 2L, "Bruno Buyer"))
    }

    private fun stubRoundTrip(
        outboundId: Long,
        returnId: Long,
        origin: String,
        destination: String,
        returnOrigin: String = destination,
        returnDestination: String = origin,
    ) {
        Mockito.`when`(flightsClient.findById(outboundId)).thenReturn(flight(outboundId, origin, destination))
        Mockito.`when`(flightsClient.findById(returnId)).thenReturn(flight(returnId, returnOrigin, returnDestination))
    }

    private fun flight(id: Long, origin: String, destination: String): ExternalFlightDto {
        return ExternalFlightDto(
            id = id,
            airline = "Aerolineas Argentinas",
            flightDate = LocalDate.of(2026, 12, 1),
            departureTime = LocalTime.of(8, 0),
            origin = origin,
            destination = destination,
            capacity = 180,
            availability = 180,
        )
    }
}
