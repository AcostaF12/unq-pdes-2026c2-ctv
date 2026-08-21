package unq.pdes.backend.tests.unit.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class UserTest {

    private fun validBuilder(): User.Builder {
        return User.Builder()
            .username("jdoe")
            .password("secret")
            .role(Role.BUYER)
            .firstName("John")
            .lastName("Doe")
    }

    @Test
    fun `01 - builder should create a valid user`() {
        val user = validBuilder().id(1L).build()

        assertEquals(1L, user.id)
        assertEquals("jdoe", user.username)
        assertEquals("secret", user.password)
        assertEquals(Role.BUYER, user.role)
        assertEquals("John", user.firstName)
        assertEquals("Doe", user.lastName)
    }

    @Test
    fun `02 - blank username should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            User.Builder().username("  ")
        }

        assertEquals("The user must have a username.", exception.message)
    }

    @Test
    fun `03 - blank password should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            User.Builder().password("")
        }

        assertEquals("The user must have a password.", exception.message)
    }

    @Test
    fun `04 - blank first name should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            User.Builder().firstName(" ")
        }

        assertEquals("The user must have a first name.", exception.message)
    }

    @Test
    fun `05 - blank last name should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            User.Builder().lastName(" ")
        }

        assertEquals("The user must have a last name.", exception.message)
    }

    @Test
    fun `06 - building without username should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            User.Builder().password("secret").role(Role.BUYER).firstName("John").lastName("Doe").build()
        }

        assertEquals("The user must have a username.", exception.message)
    }

    @Test
    fun `07 - building without role should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            User.Builder().username("jdoe").password("secret").firstName("John").lastName("Doe").build()
        }

        assertEquals("The user must have a role.", exception.message)
    }

    @Test
    fun `08 - AGENCY role should not be allowed for a standard user`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            User.Builder().role(Role.AGENCY)
        }

        assertEquals("AGENCY users must be built with AgencyUser.Builder.", exception.message)
    }

    @Test
    fun `09 - users with same id should be equal`() {
        val user1 = validBuilder().id(1L).build()
        val user2 = validBuilder().id(1L).username("other").build()

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }

    @Test
    fun `10 - users with different id should not be equal`() {
        val user1 = validBuilder().id(1L).build()
        val user2 = validBuilder().id(2L).build()

        assertNotEquals(user1, user2)
    }

    @Test
    fun `11 - user hashcode without id should be zero`() {
        val user = validBuilder().build()

        assertEquals(0, user.hashCode())
    }

    @Test
    fun `12 - user should not be equal to other object type`() {
        assertNotEquals(validBuilder().id(1L).build(), "Not a user")
    }

    @Test
    fun `13 - toString should contain the username and role`() {
        val text = validBuilder().id(1L).build().toString()

        assertTrue(text.contains("username='jdoe'"))
        assertTrue(text.contains("role=BUYER"))
    }
}
