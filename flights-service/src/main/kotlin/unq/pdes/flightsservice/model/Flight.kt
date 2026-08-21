package unq.pdes.flightsservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "flights")
class Flight private constructor(builder: Builder) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = builder.id

    @Column(nullable = false, length = 120)
    val airline: String = builder.airline!!

    @Column(name = "flight_date", nullable = false)
    val flightDate: LocalDate = builder.flightDate!!

    @Column(name = "departure_time", nullable = false)
    val departureTime: LocalTime = builder.departureTime!!

    @Column(nullable = false, length = 3)
    val origin: String = builder.origin!!

    @Column(nullable = false, length = 3)
    val destination: String = builder.destination!!

    @Column(nullable = false)
    val capacity: Int = builder.capacity!!

    @Column(nullable = false)
    val availability: Int = builder.availability!!

    override fun equals(other: Any?): Boolean {
        return (other is Flight) && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Flight(id=$id, airline='$airline', origin='$origin', destination='$destination', " +
            "date=$flightDate, time=$departureTime, capacity=$capacity, availability=$availability)"
    }

    class Builder {
        var id: Long? = null
        var airline: String? = null
        var flightDate: LocalDate? = null
        var departureTime: LocalTime? = null
        var origin: String? = null
        var destination: String? = null
        var capacity: Int? = null
        var availability: Int? = null

        fun id(id: Long?) = apply {
            this.id = id
        }

        fun airline(airline: String) = apply {
            require(airline.isNotBlank()) { "The flight must have an airline." }
            this.airline = airline
        }

        fun flightDate(flightDate: LocalDate) = apply {
            this.flightDate = flightDate
        }

        fun departureTime(departureTime: LocalTime) = apply {
            this.departureTime = departureTime
        }

        fun origin(origin: String) = apply {
            require(origin.isNotBlank()) { "The flight must have an origin." }
            this.origin = origin
        }

        fun destination(destination: String) = apply {
            require(destination.isNotBlank()) { "The flight must have a destination." }
            this.destination = destination
        }

        fun capacity(capacity: Int) = apply {
            require(capacity > 0) { "The flight capacity must be greater than zero." }
            this.capacity = capacity
        }

        fun availability(availability: Int) = apply {
            require(availability >= 0) { "The flight availability cannot be negative." }
            this.availability = availability
        }

        fun build(): Flight {
            requireNotNull(airline) { "The flight must have an airline." }
            requireNotNull(flightDate) { "The flight must have a date." }
            requireNotNull(departureTime) { "The flight must have a departure time." }
            requireNotNull(origin) { "The flight must have an origin." }
            requireNotNull(destination) { "The flight must have a destination." }
            val capacityValue = requireNotNull(capacity) { "The flight must have a capacity." }
            val availabilityValue = requireNotNull(availability) { "The flight must have an availability." }
            require(origin != destination) { "Origin and destination must be different." }
            require(availabilityValue <= capacityValue) { "Availability cannot exceed capacity." }
            return Flight(this)
        }
    }
}
