package unq.pdes.flightsservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun flightsOpenAPI(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Flights Service API")
                .description("API de vuelos para la aplicación CTV")
                .version("v0.0.1"),
        )
}
