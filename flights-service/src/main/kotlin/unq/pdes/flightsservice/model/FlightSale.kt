package unq.pdes.flightsservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "flight_sales")
class FlightSale private constructor(builder: Builder) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = builder.id

    @ManyToOne(optional = false)
    @JoinColumn(name = "flight_id", nullable = false)
    val flight: Flight = builder.flight!!

    @Column(name = "passenger_name", nullable = false, length = 160)
    val passengerName: String = builder.passengerName!!

    override fun equals(other: Any?): Boolean {
        return (other is FlightSale) && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "FlightSale(id=$id, flight=${flight.id}, passengerName='$passengerName')"
    }

    class Builder {
        var id: Long? = null
        var flight: Flight? = null
        var passengerName: String? = null

        fun id(id: Long?) = apply {
            this.id = id
        }

        fun flight(flight: Flight) = apply {
            this.flight = flight
        }

        fun passengerName(passengerName: String) = apply {
            require(passengerName.isNotBlank()) { "The sale must have a passenger name." }
            this.passengerName = passengerName
        }

        fun build(): FlightSale {
            requireNotNull(flight) { "The sale must reference a flight." }
            requireNotNull(passengerName) { "The sale must have a passenger name." }
            return FlightSale(this)
        }
    }
}
