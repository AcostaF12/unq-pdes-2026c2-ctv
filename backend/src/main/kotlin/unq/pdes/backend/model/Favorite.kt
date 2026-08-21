package unq.pdes.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import unq.pdes.backend.model.user.User

@Entity
@Table(
    name = "favorites",
    uniqueConstraints = [UniqueConstraint(columnNames = ["buyer_id", "package_id"])],
)
class Favorite private constructor(builder: Builder) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = builder.id

    @ManyToOne(optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    var buyer: User = builder.buyer!!

    @ManyToOne(optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    var travelPackage: TravelPackage = builder.travelPackage!!

    override fun equals(other: Any?): Boolean {
        return (other is Favorite) && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Favorite(id=$id, buyer=${buyer.id}, package=${travelPackage.id})"
    }

    class Builder {
        var id: Long? = null
        var buyer: User? = null
        var travelPackage: TravelPackage? = null

        fun id(id: Long?) = apply {
            this.id = id
        }

        fun buyer(buyer: User) = apply {
            this.buyer = buyer
        }

        fun travelPackage(travelPackage: TravelPackage) = apply {
            this.travelPackage = travelPackage
        }

        fun build(): Favorite {
            requireNotNull(buyer) { "The favorite must have a buyer." }
            requireNotNull(travelPackage) { "The favorite must reference a package." }
            return Favorite(this)
        }
    }
}
