package unq.pdes.backend.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun ctvOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Compra Tu Viaje")
                    .description(DESCRIPTION)
                    .version("2026c2"),
            ).components(
                Components().addSecuritySchemes(
                    BEARER,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token de POST /auth/login. En Authorize, pegarlo sin el prefijo Bearer."),
                ),
            ).addSecurityItem(SecurityRequirement().addList(BEARER))

    companion object {
        const val BEARER = "bearer"

        val DESCRIPTION =
            """
            API del backend. El registro y el login no piden token. El resto sí.

            POST /auth/login devuelve un JWT. En Swagger, Authorize, y pegar solo el token.

            Comprar, favoritos y reseñas piden rol BUYER. Publicar paquetes pide AGENCY o ADMIN. Cargar vuelos y escribir hoteles piden ADMIN. Buscar paquetes lo puede hacer cualquier usuario autenticado.

            Un error llega como ErrorDto: httpCode, httpStatus y errorData.description. Los vuelos no viven en esta base. El backend los consulta por HTTP en flights-service. Esa otra API no publica Swagger.
            """.trimIndent()
    }
}
