package unq.pdes.backend.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import javax.crypto.SecretKey
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.User

@Service
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long,
) {

    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(user: User): String {
        val now = Date()
        return Jwts.builder()
            .subject(user.username)
            .claim("role", user.role.name)
            .claim("agencyId", (user as? AgencyUser)?.agency?.id)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .signWith(key)
            .compact()
    }

    fun extractUsername(token: String): String {
        return parseClaims(token).subject
    }

    fun isValid(token: String, userDetails: UserDetails): Boolean {
        return try {
            val claims = parseClaims(token)
            claims.subject == userDetails.username && claims.expiration.after(Date())
        } catch (ex: Exception) {
            false
        }
    }

    private fun parseClaims(token: String) =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
