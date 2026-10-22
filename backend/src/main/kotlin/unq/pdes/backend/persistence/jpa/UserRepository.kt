package unq.pdes.backend.persistence.jpa

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import unq.pdes.backend.model.user.User

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): User?

    fun existsByUsername(username: String): Boolean

    // Serialize administrative changes, including changes made by another administrator.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = unq.pdes.backend.model.user.Role.ADMIN order by u.id")
    fun lockAdministrators(): List<User>

    // A role transition also changes the SINGLE_TABLE subtype. Keep the same id and
    // historical foreign keys, then clear Hibernate's cached instance of the old subtype.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = """
            update users set role = :role, user_type = :userType, agency_id = :agencyId,
                first_name = :firstName, last_name = :lastName, enabled = :enabled where id = :id
        """,
        nativeQuery = true,
    )
    fun updateManagedAccount(
        id: Long,
        role: String,
        userType: String,
        agencyId: Long?,
        firstName: String,
        lastName: String,
        enabled: Boolean,
    ): Int
}
