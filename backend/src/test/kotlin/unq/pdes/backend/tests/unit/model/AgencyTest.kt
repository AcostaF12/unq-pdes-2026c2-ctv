package unq.pdes.backend.tests.unit.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.model.Agency

@TestInstance(PER_CLASS)
class AgencyTest {

    @Test
    fun `01 - builder should create a valid agency`() {
        val agency = Agency.Builder().id(1L).name("Despegar").build()

        assertEquals(1L, agency.id)
        assertEquals("Despegar", agency.name)
    }

    @Test
    fun `02 - blank name should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Agency.Builder().name("  ")
        }

        assertEquals("The agency must have a name.", exception.message)
    }

    @Test
    fun `03 - building without name should throw IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Agency.Builder().build()
        }

        assertEquals("The agency must have a name.", exception.message)
    }

    @Test
    fun `04 - agencies with same id should be equal`() {
        val agency1 = Agency.Builder().id(1L).name("Despegar").build()
        val agency2 = Agency.Builder().id(1L).name("Almundo").build()

        assertEquals(agency1, agency2)
        assertEquals(agency1.hashCode(), agency2.hashCode())
    }

    @Test
    fun `05 - agencies with different id should not be equal`() {
        val agency1 = Agency.Builder().id(1L).name("Despegar").build()
        val agency2 = Agency.Builder().id(2L).name("Despegar").build()

        assertNotEquals(agency1, agency2)
    }
}
