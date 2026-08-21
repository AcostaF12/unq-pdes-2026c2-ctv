package unq.pdes.flightsservice.controller.dtos.responses

import java.time.LocalDateTime

data class ErrorDto(
    val httpCode: Int,
    val httpStatus: String,
    val errorData: ErrorDataDto,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {

    data class ErrorDataDto(val description: String)
}
