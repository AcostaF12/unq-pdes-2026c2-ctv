package unq.pdes.backend.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Reporta el estado de flights-service (servicio interno) dentro del /actuator/health del backend.
 * Deshabilitado en el perfil de test, donde flights-service no está corriendo.
 */
@Component
@Profile("!test")
class FlightsServiceHealthIndicator(
	@Value("\${flights.service.url}") private val flightsServiceUrl: String,
) : HealthIndicator {

	private val restClient = RestClient.create()

	override fun health(): Health {
		return try {
			val body = restClient.get()
				.uri("$flightsServiceUrl/actuator/health")
				.retrieve()
				.body(String::class.java)

			if (body != null && body.contains("\"status\":\"UP\"")) {
				Health.up().withDetail("url", flightsServiceUrl).build()
			} else {
				Health.down().withDetail("url", flightsServiceUrl).build()
			}
		} catch (ex: Exception) {
			Health.down(ex).withDetail("url", flightsServiceUrl).build()
		}
	}
}
