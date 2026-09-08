package unq.pdes.backend.external.flights

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

@Component
class FlightsClient(
    @Value("\${flights.service.url:http://localhost:8081}") private val flightsServiceUrl: String,
    restClientBuilder: RestClient.Builder,
) {

    private val restClient = restClientBuilder.build()

    @Retry(name = "flightsService")
    @CircuitBreaker(name = "flightsService")
    fun findById(id: Long): ExternalFlightDto {
        return execute("There is no Flight with id: $id.") {
            restClient.get()
                .uri("$flightsServiceUrl/flights/{id}", id)
                .retrieve()
                .body<ExternalFlightDto>()
        } ?: throw EntityNotFoundException("There is no Flight with id: $id.")
    }

    @Retry(name = "flightsService")
    @CircuitBreaker(name = "flightsService")
    fun sell(flightId: Long, passengerName: String): ExternalFlightSaleDto {
        return execute("Could not sell a seat for flight $flightId.") {
            restClient.post()
                .uri("$flightsServiceUrl/flights/{id}/sales", flightId)
                .body(mapOf("passengerName" to passengerName))
                .retrieve()
                .body<ExternalFlightSaleDto>()
        } ?: throw FlightsServiceUnavailableException("Could not sell a seat for flight $flightId.")
    }

    @Retry(name = "flightsService")
    @CircuitBreaker(name = "flightsService")
    fun cancelSale(saleId: Long) {
        execute("There is no FlightSale with id: $saleId.") {
            restClient.delete()
                .uri("$flightsServiceUrl/flights/sales/{id}", saleId)
                .retrieve()
                .toBodilessEntity()
        }
    }

    private fun <T> execute(notFoundMessage: String, block: () -> T): T {
        return try {
            block()
        } catch (ex: RestClientResponseException) {
            throw mapResponseException(ex, notFoundMessage)
        } catch (ex: ResourceAccessException) {
            throw FlightsServiceUnavailableException("Flights service is unavailable.", ex)
        }
    }

    private fun mapResponseException(
        ex: RestClientResponseException,
        notFoundMessage: String,
    ): RuntimeException {
        val description = parseDescription(ex.responseBodyAsString) ?: ex.statusText
        return when {
            ex.statusCode == HttpStatus.NOT_FOUND -> EntityNotFoundException(
                description.ifBlank { notFoundMessage },
            )
            ex.statusCode.is4xxClientError -> IllegalArgumentException(description)
            else -> FlightsServiceUnavailableException(description, ex)
        }
    }

    private fun parseDescription(body: String): String? {
        val marker = "\"description\":\""
        val start = body.indexOf(marker)
        if (start < 0) {
            return null
        }
        val from = start + marker.length
        val end = body.indexOf('"', from)
        if (end < 0) {
            return null
        }
        return body.substring(from, end)
    }
}
