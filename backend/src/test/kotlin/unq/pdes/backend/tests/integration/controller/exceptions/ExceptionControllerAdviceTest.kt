package unq.pdes.backend.tests.integration.controller.exceptions

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import unq.pdes.backend.controller.exceptions.ExceptionControllerAdvice

@TestInstance(PER_CLASS)
class ExceptionControllerAdviceTest {

    private lateinit var mvc: MockMvc

    @BeforeAll
    fun setUp() {
        mvc = MockMvcBuilders.standaloneSetup(TestExceptionController())
            .setControllerAdvice(ExceptionControllerAdvice())
            .build()
    }

    @Test
    fun `01 - should handle IllegalArgumentException with 400 status code`() {
        mvc.perform(get("/test/illegal-argument").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.httpCode").value(400))
            .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.errorData.errorDescription").value("Invalid argument message"))
    }

    @Test
    fun `02 - should handle EntityNotFoundException with 404 status code`() {
        mvc.perform(get("/test/entity-not-found").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.httpCode").value(404))
            .andExpect(jsonPath("$.httpStatus").value("NOT_FOUND"))
            .andExpect(jsonPath("$.errorData.errorDescription").value("Entity not found message"))
    }

    @Test
    fun `03 - should handle RuntimeException with 500 status code`() {
        mvc.perform(get("/test/runtime-exception").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.httpCode").value(500))
            .andExpect(jsonPath("$.httpStatus").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.errorData.errorDescription").value("An unexpected error has occurred."))
    }
}

@RestController
private class TestExceptionController {

    @GetMapping("/test/illegal-argument")
    fun throwIllegalArgumentException() {
        throw IllegalArgumentException("Invalid argument message")
    }

    @GetMapping("/test/entity-not-found")
    fun throwEntityNotFoundException() {
        throw EntityNotFoundException("Entity not found message")
    }

    @GetMapping("/test/runtime-exception")
    fun throwRuntimeException() {
        throw RuntimeException("This message will be replaced by the generic message")
    }
}
