package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User
import unq.pdes.backend.persistence.jpa.UserRepository

@Service
class UserAdministrationService(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val agencyService: AgencyService,
) {
    @Transactional(readOnly = true)
    fun list(
        page: Int,
        size: Int,
    ): Page<User> {
        require(page >= 0 && size in 1..100) { "Página inválida. El tamaño debe estar entre 1 y 100." }
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("username")))
    }

    @Transactional
    fun create(
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        role: Role,
        agencyId: Long?,
    ): User {
        require(password.toByteArray(Charsets.UTF_8).size <= 72) { "La contraseña supera los 72 bytes." }
        validateAgency(role, agencyId)
        val normalizedFirstName = firstName.trim()
        val normalizedLastName = lastName.trim()
        val user =
            if (role == Role.AGENCY) {
                userService.createAgencyUser(
                    username,
                    password,
                    normalizedFirstName,
                    normalizedLastName,
                    agencyService.findById(agencyId!!),
                )
            } else if (role == Role.ADMIN) {
                userService.createAdmin(username, password, normalizedFirstName, normalizedLastName)
            } else {
                userService.register(username, password, normalizedFirstName, normalizedLastName)
            }
        return user
    }

    @Transactional
    fun update(
        actorUsername: String,
        id: Long,
        firstName: String,
        lastName: String,
        role: Role,
        enabled: Boolean,
        agencyId: Long?,
    ): User {
        val actor =
            userRepository.lockAdministrators().find { it.username == actorUsername && it.enabled }
                ?: throw AccessDeniedException("Solo un administrador activo puede gestionar usuarios.")
        val target = userRepository.findById(id).orElseThrow { EntityNotFoundException("No existe el usuario $id.") }
        require(target.id != actor.id || (enabled && role == Role.ADMIN)) {
            "No podés deshabilitar tu propia cuenta ni quitarte el rol de administrador."
        }
        validateAgency(role, agencyId)
        target.updateProfile(firstName, lastName)
        userRepository.updateManagedAccount(
            id,
            role.name,
            if (role == Role.AGENCY) "AGENCY" else "STANDARD",
            agencyId,
            target.firstName,
            target.lastName,
            enabled,
        )
        return userRepository.findById(id).orElseThrow()
    }

    private fun validateAgency(
        role: Role,
        agencyId: Long?,
    ) {
        require((role == Role.AGENCY) == (agencyId != null)) {
            "Un vendedor debe pertenecer a una agencia. Los otros roles no deben indicar agencia."
        }
        agencyId?.let { agencyService.findById(it) }
    }
}
