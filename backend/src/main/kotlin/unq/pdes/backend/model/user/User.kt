package unq.pdes.backend.model.user

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("STANDARD")
open class User protected constructor(builder: BaseBuilder<*>) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = builder.id

    @Column(nullable = false, unique = true, length = 60)
    var username: String = builder.username!!

    @Column(nullable = false, length = 100)
    var password: String = builder.password!!

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: Role = builder.role!!

    @Column(name = "first_name", nullable = false, length = 80)
    var firstName: String = builder.firstName!!

    @Column(name = "last_name", nullable = false, length = 80)
    var lastName: String = builder.lastName!!

    fun updateProfile(firstName: String, lastName: String) {
        require(firstName.isNotBlank()) { "The user must have a first name." }
        require(lastName.isNotBlank()) { "The user must have a last name." }
        this.firstName = firstName.trim()
        this.lastName = lastName.trim()
    }

    fun changePassword(encodedPassword: String) {
        require(encodedPassword.isNotBlank()) { "The user must have a password." }
        this.password = encodedPassword
    }

    override fun equals(other: Any?): Boolean {
        return (other is User) && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "User(id=$id, username='$username', role=$role)"
    }

    abstract class BaseBuilder<SELF : BaseBuilder<SELF>> {
        var id: Long? = null
        var username: String? = null
        var password: String? = null
        var role: Role? = null
        var firstName: String? = null
        var lastName: String? = null

        @Suppress("UNCHECKED_CAST")
        protected fun self(): SELF = this as SELF

        fun id(id: Long?): SELF = self().apply { this.id = id }

        fun username(username: String): SELF = self().apply {
            require(username.isNotBlank()) { "The user must have a username." }
            this.username = username
        }

        fun password(password: String): SELF = self().apply {
            require(password.isNotBlank()) { "The user must have a password." }
            this.password = password
        }

        fun firstName(firstName: String): SELF = self().apply {
            require(firstName.isNotBlank()) { "The user must have a first name." }
            this.firstName = firstName
        }

        fun lastName(lastName: String): SELF = self().apply {
            require(lastName.isNotBlank()) { "The user must have a last name." }
            this.lastName = lastName
        }

        protected fun validateCommonFields() {
            requireNotNull(username) { "The user must have a username." }
            requireNotNull(password) { "The user must have a password." }
            requireNotNull(firstName) { "The user must have a first name." }
            requireNotNull(lastName) { "The user must have a last name." }
        }
    }

    class Builder : BaseBuilder<Builder>() {

        fun role(role: Role): Builder = self().apply {
            require(role != Role.AGENCY) { "AGENCY users must be built with AgencyUser.Builder." }
            this.role = role
        }

        fun build(): User {
            validateCommonFields()
            requireNotNull(role) { "The user must have a role." }
            return User(this)
        }
    }
}
