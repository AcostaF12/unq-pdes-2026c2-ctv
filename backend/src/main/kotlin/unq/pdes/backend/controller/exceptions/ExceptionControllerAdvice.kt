package unq.pdes.backend.controller.exceptions

import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import unq.pdes.backend.controller.dtos.responses.ErrorDto
import unq.pdes.backend.external.flights.FlightsServiceUnavailableException

@ControllerAdvice
class ExceptionControllerAdvice {
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConflict(ex: DataIntegrityViolationException): ResponseEntity<ErrorDto> =
        buildResponse(HttpStatus.CONFLICT, "La operación entra en conflicto con los datos existentes.")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorDto> =
        buildResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request.")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorDto> {
        val message =
            ex.bindingResult.fieldErrors
                .firstOrNull()
                ?.defaultMessage
                ?: ex.bindingResult.globalErrors
                    .firstOrNull()
                    ?.defaultMessage
                ?: "Invalid request."
        return buildResponse(HttpStatus.BAD_REQUEST, message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ErrorDto> =
        buildResponse(HttpStatus.BAD_REQUEST, "The request body is malformed or incomplete.")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorDto> =
        buildResponse(HttpStatus.BAD_REQUEST, "Invalid value for parameter '${ex.name}'.")

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ErrorDto> =
        buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password.")

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<ErrorDto> =
        buildResponse(HttpStatus.NOT_FOUND, ex.message ?: "Entity not found.")

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ErrorDto> =
        buildResponse(HttpStatus.FORBIDDEN, ex.message ?: "Access denied.")

    @ExceptionHandler(FlightsServiceUnavailableException::class)
    fun handleFlightsServiceUnavailableException(ex: FlightsServiceUnavailableException): ResponseEntity<ErrorDto> =
        buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.message ?: "Flights service is unavailable.")

    @ExceptionHandler(RuntimeException::class)
    fun handleGenericException(ex: RuntimeException): ResponseEntity<ErrorDto> =
        buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred.")

    private fun buildResponse(
        status: HttpStatus,
        description: String,
    ): ResponseEntity<ErrorDto> {
        val errorDto =
            ErrorDto(
                httpCode = status.value(),
                httpStatus = status.name,
                errorData = ErrorDto.ErrorDataDto(description),
            )
        return ResponseEntity.status(status).body(errorDto)
    }
}
