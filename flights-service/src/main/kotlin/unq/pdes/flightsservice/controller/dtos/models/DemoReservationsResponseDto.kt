package unq.pdes.flightsservice.controller.dtos.models

import unq.pdes.flightsservice.model.FlightSale

data class DemoReservationResultDto(
    val key: String,
    val saleId: Long,
)

data class DemoReservationsResponseDto(
    val reservations: List<DemoReservationResultDto>,
) {
    companion object {
        fun from(sales: List<FlightSale>): DemoReservationsResponseDto =
            DemoReservationsResponseDto(sales.map { DemoReservationResultDto(it.seedKey!!, it.id!!) })
    }
}
