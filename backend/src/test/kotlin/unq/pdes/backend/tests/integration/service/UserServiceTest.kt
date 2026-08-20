package unq.pdes.backend.tests.integration.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.persistence.jpa.AgencyRepository
import unq.pdes.backend.service.UserService

@SpringBootTest
class UserServiceTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var agencyRepository: AgencyRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var dataServiceH2: unq.pdes.backend.helpers.service.DataServiceH2

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - register should create a BUYER with a hashed password`() {
        val user = userService.register("jdoe", "secret", "John", "Doe")

        assertNotNull(user.id)
        assertEquals("jdoe", user.username)
        assertEquals(Role.BUYER, user.role)
        assertNotEquals("secret", user.password)
        assertTrue(passwordEncoder.matches("secret", user.password))
    }

    @Test
    fun `02 - createAdmin should create an ADMIN`() {
        val user = userService.createAdmin("facosta", "facosta", "Federico", "Acosta")

        assertEquals(Role.ADMIN, user.role)
        assertEquals("facosta", user.username)
    }

    @Test
    fun `03 - createAgencyUser should create an AgencyUser linked to the agency`() {
        val agency = agencyRepository.save(Agency.Builder().name("Despegar").build())

        val user = userService.createAgencyUser("agency", "secret", "Agus", "Agency", agency)

        assertTrue(user is AgencyUser)
        assertEquals(Role.AGENCY, user.role)
        assertEquals(agency.id, user.agency.id)
    }

    @Test
    fun `04 - registering a taken username should throw IllegalArgumentException`() {
        userService.register("jdoe", "secret", "John", "Doe")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.register("jdoe", "other", "Jane", "Doe")
        }

        assertEquals("The username 'jdoe' is already taken.", exception.message)
    }

    @Test
    fun `05 - findByUsername should return the persisted user`() {
        userService.register("jdoe", "secret", "John", "Doe")

        val found = userService.findByUsername("jdoe")

        assertEquals("jdoe", found.username)
    }

    @Test
    fun `06 - findByUsername should throw EntityNotFoundException when not found`() {
        val exception = assertThrows(EntityNotFoundException::class.java) {
            userService.findByUsername("ghost")
        }

        assertEquals("There is no user with username: ghost.", exception.message)
    }
}
