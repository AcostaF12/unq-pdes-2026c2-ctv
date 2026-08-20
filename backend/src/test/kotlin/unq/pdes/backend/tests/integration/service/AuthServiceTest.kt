package unq.pdes.backend.tests.integration.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.BadCredentialsException
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.service.AuthService

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - register should return a token and the created buyer`() {
        val (token, user) = authService.register("jdoe", "secret", "John", "Doe")

        assertTrue(token.isNotBlank())
        assertEquals("jdoe", user.username)
        assertEquals(Role.BUYER, user.role)
    }

    @Test
    fun `02 - login with valid credentials should return a token and the user`() {
        authService.register("jdoe", "secret", "John", "Doe")

        val (token, user) = authService.login("jdoe", "secret")

        assertTrue(token.isNotBlank())
        assertEquals("jdoe", user.username)
    }

    @Test
    fun `03 - login with wrong password should throw BadCredentialsException`() {
        authService.register("jdoe", "secret", "John", "Doe")

        assertThrows(BadCredentialsException::class.java) {
            authService.login("jdoe", "wrong")
        }
    }

    @Test
    fun `04 - login with unknown user should throw BadCredentialsException`() {
        assertThrows(BadCredentialsException::class.java) {
            authService.login("ghost", "secret")
        }
    }
}
