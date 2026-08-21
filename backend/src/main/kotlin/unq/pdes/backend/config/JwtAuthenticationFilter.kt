package unq.pdes.backend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import unq.pdes.backend.service.JwtService

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)
        if (token != null && isNotAuthenticated()) {
            authenticateWithToken(token, request)
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val header = request.getHeader(AUTHORIZATION_HEADER) ?: return null
        return if (header.startsWith(BEARER_PREFIX)) header.removePrefix(BEARER_PREFIX) else null
    }

    private fun isNotAuthenticated(): Boolean {
        return SecurityContextHolder.getContext().authentication == null
    }

    private fun authenticateWithToken(token: String, request: HttpServletRequest) {
        val username = runCatching { jwtService.extractUsername(token) }.getOrNull() ?: return
        val userDetails = userDetailsService.loadUserByUsername(username)
        if (jwtService.isValid(token, userDetails)) {
            SecurityContextHolder.getContext().authentication = buildAuthentication(userDetails, request)
        }
    }

    private fun buildAuthentication(
        userDetails: UserDetails,
        request: HttpServletRequest,
    ): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities).apply {
            details = WebAuthenticationDetailsSource().buildDetails(request)
        }
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
