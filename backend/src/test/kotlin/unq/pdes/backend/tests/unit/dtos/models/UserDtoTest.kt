package unq.pdes.backend.tests.unit.dtos.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.controller.dtos.models.AgencyUserDto
import unq.pdes.backend.controller.dtos.models.UserDto
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User

@TestInstance(PER_CLASS)
class UserDtoTest {

    @Test
    fun `01 - fromModel of a standard user should return a plain UserDto`() {
        val user = User.Builder()
            .id(1L).username("jdoe").password("secret").role(Role.BUYER).firstName("John").lastName("Doe").build()

        val dto = UserDto.fromModel(user)

        assertFalse(dto is AgencyUserDto)
        assertEquals(1L, dto.id)
        assertEquals("jdoe", dto.username)
        assertEquals("BUYER", dto.role)
        assertEquals("John", dto.firstName)
    }

    @Test
    fun `02 - fromModel of an agency user should return an AgencyUserDto with the agency`() {
        val agency = Agency.Builder().id(5L).name("Despegar").build()
        val user = AgencyUser.Builder()
            .id(2L).username("agency").password("secret").firstName("Agus").lastName("Agency").agency(agency).build()

        val dto = UserDto.fromModel(user)

        assertTrue(dto is AgencyUserDto)
        assertEquals("AGENCY", dto.role)
        assertEquals(5L, (dto as AgencyUserDto).agency.id)
        assertEquals("Despegar", dto.agency.name)
    }
}
