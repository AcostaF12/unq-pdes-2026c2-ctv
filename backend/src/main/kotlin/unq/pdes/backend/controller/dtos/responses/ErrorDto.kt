package unq.pdes.backend.controller.dtos.responses

import java.time.LocalDateTime

data class ErrorDto(
    val httpCode: Int,
    val httpStatus: String,
    val errorData: ErrorDataDto,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {

    data class ErrorDataDto(val errorDescription: String)
}
