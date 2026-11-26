package unq.pdes.flightsservice.tests.unit.service

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.LockModeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import unq.pdes.flightsservice.model.Flight
import unq.pdes.flightsservice.model.FlightSale
import unq.pdes.flightsservice.persistence.jpa.FlightRepository
import unq.pdes.flightsservice.persistence.jpa.FlightSaleRepository
import unq.pdes.flightsservice.service.FlightService
import java.time.LocalDate
import java.time.LocalTime

class FlightServiceCancelSaleTest {
    private val flights = Mockito.mock(FlightRepository::class.java)
    private val sales = Mockito.mock(FlightSaleRepository::class.java)
    private val entityManager = Mockito.mock(EntityManager::class.java)
    private val service = FlightService(flights, sales, entityManager)

    @Test
    fun `01 - cancelSale rejects a sale whose flight has no id`() {
        val sale = sale(flight(id = null), saleId = 4)
        Mockito.`when`(sales.findForUpdate(4)).thenReturn(sale)

        val error =
            assertThrows(EntityNotFoundException::class.java) {
                service.cancelSale(4)
            }

        assertEquals("There is no Flight for sale: 4.", error.message)
    }

    @Test
    fun `02 - cancelSale rejects a sale when the flight no longer exists`() {
        val booked = flight(id = 9)
        val sale = sale(booked, saleId = 4)
        Mockito.`when`(sales.findForUpdate(4)).thenReturn(sale)
        Mockito
            .doThrow(EntityNotFoundException("gone"))
            .`when`(entityManager)
            .refresh(booked, LockModeType.PESSIMISTIC_WRITE)

        val error =
            assertThrows(EntityNotFoundException::class.java) {
                service.cancelSale(4)
            }

        assertEquals("There is no Flight with id: 9.", error.message)
        Mockito.verify(flights, Mockito.never()).findForUpdate(Mockito.anyLong())
    }

    private fun flight(id: Long?): Flight =
        Flight
            .Builder()
            .id(id)
            .airline("Aerolineas")
            .flightDate(LocalDate.of(2026, 12, 1))
            .departureTime(LocalTime.of(10, 30))
            .origin("BUE")
            .destination("PAR")
            .capacity(5)
            .availability(4)
            .build()

    private fun sale(
        flight: Flight,
        saleId: Long,
    ): FlightSale =
        FlightSale
            .Builder()
            .id(saleId)
            .flight(flight)
            .passengerName("Bruno Buyer")
            .build()
}
