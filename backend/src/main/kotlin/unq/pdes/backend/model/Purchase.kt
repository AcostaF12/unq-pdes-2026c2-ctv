package unq.pdes.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import unq.pdes.backend.model.user.User

@Entity
@Table(name = "purchases")
class Purchase private constructor(builder: Builder) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = builder.id

    @ManyToOne(optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    var buyer: User = builder.buyer!!

    @ManyToOne(optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    var travelPackage: TravelPackage = builder.travelPackage!!

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    var purchasePrice: BigDecimal = builder.purchasePrice!!

    @Column(name = "purchased_at", nullable = false)
    var purchasedAt: LocalDateTime = builder.purchasedAt!!

    override fun equals(other: Any?): Boolean {
        return (other is Purchase) && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Purchase(id=$id, buyer=${buyer.id}, package=${travelPackage.id}, " +
            "price=$purchasePrice, at=$purchasedAt)"
    }

    class Builder {
        var id: Long? = null
        var buyer: User? = null
        var travelPackage: TravelPackage? = null
        var purchasePrice: BigDecimal? = null
        var purchasedAt: LocalDateTime? = null

        fun id(id: Long?) = apply {
            this.id = id
        }

        fun buyer(buyer: User) = apply {
            this.buyer = buyer
        }

        fun travelPackage(travelPackage: TravelPackage) = apply {
            this.travelPackage = travelPackage
        }

        fun purchasePrice(purchasePrice: BigDecimal) = apply {
            require(purchasePrice.signum() > 0) { "The purchase price must be greater than zero." }
            this.purchasePrice = purchasePrice
        }

        fun purchasedAt(purchasedAt: LocalDateTime) = apply {
            this.purchasedAt = purchasedAt
        }

        fun build(): Purchase {
            requireNotNull(buyer) { "The purchase must have a buyer." }
            requireNotNull(travelPackage) { "The purchase must reference a package." }
            requireNotNull(purchasePrice) { "The purchase must have a price." }
            requireNotNull(purchasedAt) { "The purchase must have a timestamp." }
            return Purchase(this)
        }
    }
}
