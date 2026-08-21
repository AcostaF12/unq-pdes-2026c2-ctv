package unq.pdes.backend.model

import jakarta.persistence.Column
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
    name = "reviews",
    uniqueConstraints = [UniqueConstraint(columnNames = ["buyer_id", "package_id"])],
)
class Review private constructor(builder: Builder) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = builder.id

    @ManyToOne(optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    var buyer: User = builder.buyer!!

    @ManyToOne(optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    var travelPackage: TravelPackage = builder.travelPackage!!

    @Column(nullable = false)
    var score: Int = builder.score!!

    @Column(columnDefinition = "text")
    var comment: String? = builder.comment

    override fun equals(other: Any?): Boolean {
        return (other is Review) && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Review(id=$id, buyer=${buyer.id}, package=${travelPackage.id}, score=$score)"
    }

    class Builder {
        var id: Long? = null
        var buyer: User? = null
        var travelPackage: TravelPackage? = null
        var score: Int? = null
        var comment: String? = null

        fun id(id: Long?) = apply {
            this.id = id
        }

        fun buyer(buyer: User) = apply {
            this.buyer = buyer
        }

        fun travelPackage(travelPackage: TravelPackage) = apply {
            this.travelPackage = travelPackage
        }

        fun score(score: Int) = apply {
            require(score in 0..10) { "The score must be between 0 and 10." }
            this.score = score
        }

        fun comment(comment: String?) = apply {
            this.comment = comment
        }

        fun build(): Review {
            requireNotNull(buyer) { "The review must have a buyer." }
            requireNotNull(travelPackage) { "The review must reference a package." }
            requireNotNull(score) { "The review must have a score." }
            return Review(this)
        }
    }
}
