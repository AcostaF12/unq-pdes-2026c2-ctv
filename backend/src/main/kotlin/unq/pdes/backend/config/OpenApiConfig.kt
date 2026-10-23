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
                    .title("CTV - Backend API")
                    .version("v0.0.1"),
            ).components(
                Components().addSecuritySchemes(
                    BEARER_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Token de POST /auth/login o POST /auth/register. " +
                                "Pegalo solo, sin la palabra Bearer. Dura 24 horas.",
                        ),
                ),
            ).addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

    private companion object {
        const val BEARER_SCHEME = "bearer-jwt"
    }
}
