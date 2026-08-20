package unq.pdes.backend.config

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http.csrf { it.disable() }
		http.authorizeHttpRequests {
			it.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
			it.requestMatchers(
				"/v3/api-docs/**",
				"/swagger-ui/**",
				"/swagger-ui.html",
			).permitAll()
			it.requestMatchers(
				"/hotels/**",
				"/destinations/**",
			).permitAll()
			it.anyRequest().authenticated()
		}.httpBasic {}
		return http.build()
	}
}
