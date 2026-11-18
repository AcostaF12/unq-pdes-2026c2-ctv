package unq.pdes.flightsservice.tests.unit.dtos.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import unq.pdes.flightsservice.controller.dtos.models.DemoReservationResultDto
import unq.pdes.flightsservice.controller.dtos.models.DemoReservationsResponseDto
import unq.pdes.flightsservice.helpers.factory.ObjectsFactory
import unq.pdes.flightsservice.model.FlightSale

class DemoReservationsResponseDtoTest {
    private fun seededSale(
        id: Long,
        key: String,
    ): FlightSale =
        FlightSale
            .Builder()
            .id(id)
            .flight(ObjectsFactory().anyFlight())
            .passengerName("Demo Buyer")
            .build()
            .apply { seedKey = key }

    @Test
    fun `01 - should expose the sale id of every demo reservation in order`() {
        val dto =
            DemoReservationsResponseDto.from(
                listOf(seededSale(7L, "ctv-demo-v1:buyer:trip:0"), seededSale(8L, "ctv-demo-v1:buyer:trip:1")),
            )

        assertEquals(
            listOf(
                DemoReservationResultDto("ctv-demo-v1:buyer:trip:0", 7L),
                DemoReservationResultDto("ctv-demo-v1:buyer:trip:1", 8L),
            ),
            dto.reservations,
        )
    }
}
