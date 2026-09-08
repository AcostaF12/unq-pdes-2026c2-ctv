package unq.pdes.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "cities")
data class City(
    @Id
    @Column(length = 3, nullable = false)
    var code: String,

    @Column(nullable = false)
    var name: String,
) {

    override fun equals(other: Any?): Boolean {
        return (other is City) && code == other.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
}
