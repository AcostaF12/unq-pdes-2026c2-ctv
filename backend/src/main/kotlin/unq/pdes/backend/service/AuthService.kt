package unq.pdes.backend.service

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import unq.pdes.backend.model.user.User

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val jwtService: JwtService,
) {

    fun register(username: String, password: String, firstName: String, lastName: String): Pair<String, User> {
        val user = userService.register(username, password, firstName, lastName)
        return jwtService.generateToken(user) to user
    }

    fun login(username: String, password: String): Pair<String, User> {
        authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
        val user = userService.findByUsername(username)
        return jwtService.generateToken(user) to user
    }
}
