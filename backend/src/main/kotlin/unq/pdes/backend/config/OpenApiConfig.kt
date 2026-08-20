package unq.pdes.backend.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

	@Bean
	fun ctvOpenAPI(): OpenAPI =
		OpenAPI().info(
			Info()
				.title("CTV - Backend API")
				.description("API de la aplicación Compra Tu Viaje (CTV)")
				.version("v0.0.1"),
		)
}
