package unq.pdes.backend.external.flights

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
@Profile("!test")
class FlightsServiceHealthIndicator(
	@Value("\${flights.service.url}") private val flightsServiceUrl: String,
) : HealthIndicator {

	private val logger = LoggerFactory.getLogger(javaClass)
	private val restClient = RestClient.create()

	override fun health(): Health {
		logger.info("Checking external flights service health at {}", flightsServiceUrl)

		return try {
			val body = restClient.get()
				.uri("$flightsServiceUrl/actuator/health")
				.retrieve()
				.body<String>()

			if (body != null && body.contains("\"status\":\"UP\"")) {
				logger.debug("Flights service health check succeeded for {}", flightsServiceUrl)
				Health.up().withDetail("url", flightsServiceUrl).build()
			} else {
				logger.warn("Flights service health check returned non-UP status for {}", flightsServiceUrl)
				Health.down().withDetail("url", flightsServiceUrl).build()
			}
		} catch (ex: Exception) {
			logger.error("Flights service health check failed for {}", flightsServiceUrl, ex)
			Health.down(ex).withDetail("url", flightsServiceUrl).build()
		}
	}
}
