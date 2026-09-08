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
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.service.TravelPackageService

@SpringBootTest
class TravelPackageServiceTest {

    @Autowired
    private lateinit var travelPackageService: TravelPackageService

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

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
