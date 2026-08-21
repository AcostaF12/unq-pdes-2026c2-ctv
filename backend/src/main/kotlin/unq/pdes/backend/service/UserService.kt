package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User
import unq.pdes.backend.persistence.jpa.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional(readOnly = true)
    fun findByUsername(username: String): User {
        return userRepository.findByUsername(username)
            ?: throw EntityNotFoundException("There is no user with username: $username.")
    }

    @Transactional
    fun register(username: String, rawPassword: String, firstName: String, lastName: String): User {
        return createStandard(username, rawPassword, Role.BUYER, firstName, lastName)
    }

    @Transactional
    fun createAdmin(username: String, rawPassword: String, firstName: String, lastName: String): User {
        return createStandard(username, rawPassword, Role.ADMIN, firstName, lastName)
    }

    @Transactional
    fun createAgencyUser(
        username: String,
        rawPassword: String,
        firstName: String,
        lastName: String,
        agency: Agency,
    ): AgencyUser {
        requireUsernameAvailable(username)
        val user = AgencyUser.Builder()
            .username(username)
            .password(passwordEncoder.encode(rawPassword)!!)
            .firstName(firstName)
            .lastName(lastName)
            .agency(agency)
            .build()
        return userRepository.save(user)
    }

    private fun createStandard(
        username: String,
        rawPassword: String,
        role: Role,
        firstName: String,
        lastName: String,
    ): User {
        requireUsernameAvailable(username)
        val user = User.Builder()
            .username(username)
            .password(passwordEncoder.encode(rawPassword)!!)
            .role(role)
            .firstName(firstName)
            .lastName(lastName)
            .build()
        return userRepository.save(user)
    }

    private fun requireUsernameAvailable(username: String) {
        require(!userRepository.existsByUsername(username)) {
            "The username '$username' is already taken."
        }
    }
}
