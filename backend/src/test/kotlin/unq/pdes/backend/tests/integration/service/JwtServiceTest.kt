package unq.pdes.backend.tests.integration.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User as SpringUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private lateinit var jwtService: unq.pdes.backend.service.JwtService

    private fun userNamed(username: String): User {
        return User.Builder()
            .username(username).password("secret").role(Role.BUYER).firstName("John").lastName("Doe").build()
    }

    private fun details(username: String) =
        SpringUser(username, "secret", AuthorityUtils.createAuthorityList("ROLE_BUYER"))

    @Test
    fun `01 - generated token should carry the username as subject`() {
        val token = jwtService.generateToken(userNamed("jdoe"))

        assertEquals("jdoe", jwtService.extractUsername(token))
    }

    @Test
    fun `02 - a valid token should be valid for the matching user details`() {
        val token = jwtService.generateToken(userNamed("jdoe"))

        assertTrue(jwtService.isValid(token, details("jdoe")))
    }

    @Test
    fun `03 - a token should be invalid for a different username`() {
        val token = jwtService.generateToken(userNamed("jdoe"))

        assertFalse(jwtService.isValid(token, details("someone-else")))
    }

    @Test
    fun `04 - a tampered token should be invalid`() {
        assertFalse(jwtService.isValid("not-a-real-token", details("jdoe")))
    }
}
