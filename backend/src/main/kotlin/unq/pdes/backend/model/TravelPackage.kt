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
import java.math.BigDecimal

@Entity
@Table(
    name = "packages",
    uniqueConstraints = [UniqueConstraint(columnNames = ["agency_id", "name"])],
)
class TravelPackage private constructor(builder: Builder) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = builder.id

    @ManyToOne(optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    var agency: Agency = builder.agency!!

    @ManyToOne(optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    var hotel: Hotel = builder.hotel!!

    @ManyToOne(optional = false)
    @JoinColumn(name = "origin_city_code", nullable = false)
    var origin: City = builder.origin!!

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_city_code", nullable = false)
    var destination: City = builder.destination!!

    @Column(nullable = false, length = 160)
    var name: String = builder.name!!

    @Column(name = "outbound_flight_id", nullable = false)
    var outboundFlightId: Long = builder.outboundFlightId!!

    @Column(name = "return_flight_id", nullable = false)
    var returnFlightId: Long = builder.returnFlightId!!

    @Column(nullable = false, precision = 12, scale = 2)
    var price: BigDecimal = builder.price!!

    override fun equals(other: Any?): Boolean {
        return (other is TravelPackage) && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "TravelPackage(id=$id, name='$name', agency=${agency.id}, hotel=${hotel.id}, price=$price)"
    }

    class Builder {
        var id: Long? = null
        var agency: Agency? = null
        var hotel: Hotel? = null
        var origin: City? = null
        var destination: City? = null
        var name: String? = null
        var outboundFlightId: Long? = null
        var returnFlightId: Long? = null
        var price: BigDecimal? = null

        fun id(id: Long?) = apply {
            this.id = id
        }

        fun agency(agency: Agency) = apply {
            this.agency = agency
        }

        fun hotel(hotel: Hotel) = apply {
            this.hotel = hotel
        }

        fun origin(origin: City) = apply {
            this.origin = origin
        }

        fun destination(destination: City) = apply {
            this.destination = destination
        }

        fun name(name: String) = apply {
            require(name.isNotBlank()) { "The package must have a name." }
            this.name = name
        }

        fun outboundFlightId(outboundFlightId: Long) = apply {
            this.outboundFlightId = outboundFlightId
        }

        fun returnFlightId(returnFlightId: Long) = apply {
            this.returnFlightId = returnFlightId
        }

        fun price(price: BigDecimal) = apply {
            require(price.signum() > 0) { "The package price must be greater than zero." }
            this.price = price
        }

        fun build(): TravelPackage {
            requireNotNull(agency) { "The package must belong to an agency." }
            requireNotNull(hotel) { "The package must have a hotel." }
            val originCity = requireNotNull(origin) { "The package must have an origin city." }
            val destinationCity = requireNotNull(destination) { "The package must have a destination city." }
            require(originCity.code != destinationCity.code) { "Origin and destination cities must be different." }
            requireNotNull(name) { "The package must have a name." }
            val outbound = requireNotNull(outboundFlightId) { "The package must have an outbound flight." }
            val ret = requireNotNull(returnFlightId) { "The package must have a return flight." }
            requireNotNull(price) { "The package must have a price." }
            require(outbound != ret) { "Outbound and return flights must be different." }
            return TravelPackage(this)
        }
    }
}
