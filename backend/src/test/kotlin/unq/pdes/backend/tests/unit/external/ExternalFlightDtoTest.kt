package unq.pdes.backend.tests.unit.external

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import unq.pdes.backend.external.flights.ExternalErrorDataDto
import unq.pdes.backend.external.flights.ExternalErrorDto
import unq.pdes.backend.external.flights.ExternalFlightSaleDto

@TestInstance(PER_CLASS)
class ExternalFlightDtoTest {

    @Test
    fun `01 - should create a sale dto`() {
        val sale = ExternalFlightSaleDto(id = 10L, flightId = 1L, passengerName = "Bruno Buyer")

        assertEquals(10L, sale.id)
        assertEquals(1L, sale.flightId)
        assertEquals("Bruno Buyer", sale.passengerName)
    }

    @Test
    fun `02 - should create an error dto with optional fields`() {
        val error = ExternalErrorDto(
            httpCode = 404,
            httpStatus = "NOT_FOUND",
            errorData = ExternalErrorDataDto("There is no Flight with id: 1."),
        )

        assertEquals(404, error.httpCode)
        assertEquals("NOT_FOUND", error.httpStatus)
        assertEquals("There is no Flight with id: 1.", error.errorData?.description)
    }

    @Test
    fun `03 - should create an error dto with defaults`() {
        val error = ExternalErrorDto()

        assertNull(error.httpCode)
        assertNull(error.httpStatus)
        assertNull(error.errorData)
        assertNull(ExternalErrorDataDto().description)
    }
}
