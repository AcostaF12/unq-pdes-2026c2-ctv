package unq.pdes.backend.controller.exceptions

import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import unq.pdes.backend.controller.dtos.responses.ErrorDto

@ControllerAdvice
class ExceptionControllerAdvice {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorDto> {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request.")
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<ErrorDto> {
        return buildResponse(HttpStatus.NOT_FOUND, ex.message ?: "Entity not found.")
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleGenericException(ex: RuntimeException): ResponseEntity<ErrorDto> {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred.")
    }

    private fun buildResponse(status: HttpStatus, description: String): ResponseEntity<ErrorDto> {
        val errorDto = ErrorDto(
            httpCode = status.value(),
            httpStatus = status.name,
            errorData = ErrorDto.ErrorDataDto(description),
        )
        return ResponseEntity.status(status).body(errorDto)
    }
}
