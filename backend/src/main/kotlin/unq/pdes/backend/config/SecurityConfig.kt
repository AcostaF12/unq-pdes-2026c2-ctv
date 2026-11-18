package unq.pdes.backend.config

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(configuration: AuthenticationConfiguration): AuthenticationManager = configuration.authenticationManager

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
        http.cors { }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.authorizeHttpRequests {
            it.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
            it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            it.requestMatchers("/auth/**", "/error").permitAll()
            it.requestMatchers("/admin/**").hasRole(ADMIN_ROLE)

            it.requestMatchers(HttpMethod.POST, HOTELS_PATH).hasRole(ADMIN_ROLE)
            it.requestMatchers(HttpMethod.PUT, HOTELS_PATH).hasRole(ADMIN_ROLE)
            it.requestMatchers(HttpMethod.DELETE, HOTELS_PATH).hasRole(ADMIN_ROLE)

            it.requestMatchers(HttpMethod.POST, PACKAGES_PATH).hasAnyRole(AGENCY_ROLE, ADMIN_ROLE)
            it.requestMatchers(HttpMethod.PUT, PACKAGES_PATH).hasAnyRole(AGENCY_ROLE, ADMIN_ROLE)
            it.requestMatchers(HttpMethod.DELETE, PACKAGES_PATH).hasAnyRole(AGENCY_ROLE, ADMIN_ROLE)
            it.requestMatchers(HttpMethod.GET, "/packages/agency").hasRole(AGENCY_ROLE)

            it.requestMatchers(HttpMethod.POST, "/flights").hasRole(ADMIN_ROLE)
            it.requestMatchers(HttpMethod.POST, AGENCIES_PATH).hasRole(ADMIN_ROLE)
            it.requestMatchers(HttpMethod.PUT, AGENCIES_PATH).hasRole(ADMIN_ROLE)
            it.requestMatchers(HttpMethod.DELETE, AGENCIES_PATH).hasRole(ADMIN_ROLE)

            it.requestMatchers(HttpMethod.POST, "/purchases").hasRole(BUYER_ROLE)
            it.requestMatchers(HttpMethod.GET, "/purchases/me/**").hasRole(BUYER_ROLE)
            it.requestMatchers(HttpMethod.GET, "/purchases/agency").hasRole(AGENCY_ROLE)
            it.requestMatchers(HttpMethod.DELETE, "/purchases/**").hasAnyRole(BUYER_ROLE, ADMIN_ROLE)

            it.requestMatchers(HttpMethod.POST, "/favorites").hasRole(BUYER_ROLE)
            it.requestMatchers(HttpMethod.DELETE, "/favorites/**").hasRole(BUYER_ROLE)

            it.requestMatchers(HttpMethod.POST, "/reviews").hasRole(BUYER_ROLE)

            it.anyRequest().authenticated()
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    companion object {
        private const val HOTELS_PATH = "/hotels/**"
        private const val PACKAGES_PATH = "/packages/**"
        private const val AGENCIES_PATH = "/agencies/**"
        private const val ADMIN_ROLE = "ADMIN"
        private const val AGENCY_ROLE = "AGENCY"
        private const val BUYER_ROLE = "BUYER"
    }
}
