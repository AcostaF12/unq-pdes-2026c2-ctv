package unq.pdes.backend.controller.exceptions

import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import unq.pdes.backend.controller.dtos.responses.ErrorDto
import unq.pdes.backend.external.flights.FlightsServiceUnavailableException

@ControllerAdvice
class ExceptionControllerAdvice {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorDto> {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request.")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorDto> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: ex.bindingResult.globalErrors.firstOrNull()?.defaultMessage
            ?: "Invalid request."
        return buildResponse(HttpStatus.BAD_REQUEST, message)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ErrorDto> {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password.")
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<ErrorDto> {
        return buildResponse(HttpStatus.NOT_FOUND, ex.message ?: "Entity not found.")
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ErrorDto> {
        return buildResponse(HttpStatus.FORBIDDEN, ex.message ?: "Access denied.")
    }

    @ExceptionHandler(FlightsServiceUnavailableException::class)
    fun handleFlightsServiceUnavailableException(
        ex: FlightsServiceUnavailableException,
    ): ResponseEntity<ErrorDto> {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.message ?: "Flights service is unavailable.")
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
