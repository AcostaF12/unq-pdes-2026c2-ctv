package unq.pdes.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "agencies")
class Agency private constructor(builder: Builder) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = builder.id

    @Column(nullable = false, length = 160)
    var name: String = builder.name!!

    override fun equals(other: Any?): Boolean {
        return (other is Agency) && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Agency(id=$id, name='$name')"
    }

    class Builder {
        var id: Long? = null
        var name: String? = null

        fun id(id: Long?) = apply {
            this.id = id
        }

        fun name(name: String) = apply {
            require(name.isNotBlank()) { "The agency must have a name." }
            this.name = name
        }

        fun build(): Agency {
            requireNotNull(name) { "The agency must have a name." }
            return Agency(this)
        }
    }
}
