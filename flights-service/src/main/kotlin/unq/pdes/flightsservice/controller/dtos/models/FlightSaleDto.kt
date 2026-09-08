package unq.pdes.flightsservice.controller.dtos.models

import unq.pdes.flightsservice.model.FlightSale

data class FlightSaleDto(
    val id: Long?,
    val flightId: Long?,
    val passengerName: String,
) {
    companion object {
        fun fromModel(sale: FlightSale): FlightSaleDto {
            return FlightSaleDto(
                id = sale.id,
                flightId = sale.flight.id,
                passengerName = sale.passengerName,
            )
        }
    }
}
