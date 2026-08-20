package unq.pdes.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "destinations")
data class Destination(
    @Id
    @Column(length = 3, nullable = false)
    val code: String,

    @Column(nullable = false)
    val city: String,
) {

    override fun equals(other: Any?): Boolean {
        return (other is Destination) && code == other.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
}
