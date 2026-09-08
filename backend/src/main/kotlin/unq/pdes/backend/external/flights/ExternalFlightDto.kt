package unq.pdes.backend.external.flights

import java.time.LocalDate
import java.time.LocalTime

data class ExternalFlightDto(
    val id: Long?,
    val airline: String,
    val flightDate: LocalDate,
    val departureTime: LocalTime,
    val origin: String,
    val destination: String,
    val capacity: Int,
    val availability: Int,
)

data class ExternalFlightSaleDto(
    val id: Long?,
    val flightId: Long?,
    val passengerName: String,
)

data class ExternalErrorDto(
    val httpCode: Int? = null,
    val httpStatus: String? = null,
    val errorData: ExternalErrorDataDto? = null,
)

data class ExternalErrorDataDto(
    val description: String? = null,
)
