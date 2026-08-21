package unq.pdes.backend.tests.unit.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class AgencyUserTest {

    private val agency = Agency.Builder().id(1L).name("Despegar").build()

    private fun validBuilder(): AgencyUser.Builder {
        return AgencyUser.Builder()
            .username("agency")
            .password("secret")
            .firstName("Agus")
            .lastName("Agency")
            .agency(agency)
    }

    @Test
    fun `01 - builder should create a valid agency user with AGENCY role`() {
        val user = validBuilder().id(1L).build()

        assertEquals(1L, user.id)
        assertEquals("agency", user.username)
        assertEquals(Role.AGENCY, user.role)
        assertEquals(agency, user.agency)
    }

    @Test
    fun `02 - an agency user should be a User`() {
        val user: User = validBuilder().build()

        assertTrue(user is AgencyUser)
    }

    @Test
    fun `03 - building without an agency should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            AgencyUser.Builder().username("agency").password("secret").firstName("Agus").lastName("Agency").build()
        }

        assertEquals("An agency user must belong to an agency.", exception.message)
    }

    @Test
    fun `04 - building without username should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            AgencyUser.Builder().password("secret").firstName("Agus").lastName("Agency").agency(agency).build()
        }

        assertEquals("The user must have a username.", exception.message)
    }

    @Test
    fun `05 - toString should contain the username and agency`() {
        val user = validBuilder().id(1L).build()

        assertTrue(user.toString().contains("username='agency'"))
        assertTrue(user.toString().contains("agency=1"))
    }
}
