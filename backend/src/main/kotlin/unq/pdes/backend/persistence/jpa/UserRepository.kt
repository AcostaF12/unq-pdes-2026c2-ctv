package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.backend.model.user.User

interface UserRepository : JpaRepository<User, Long> {

    fun findByUsername(username: String): User?

    fun existsByUsername(username: String): Boolean
}
