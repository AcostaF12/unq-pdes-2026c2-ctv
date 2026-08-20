package unq.pdes.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "hotels")
class Hotel private constructor(builder: Builder) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = builder.id

    @Column(nullable = false, length = 160)
    val name: String = builder.name!!

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_code", nullable = false)
    val destination: Destination = builder.destination!!

    @Column(name = "photo_url", nullable = false, length = 500)
    val photoUrl: String = builder.photoUrl!!

    override fun equals(other: Any?): Boolean {
        return (other is Hotel) && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Hotel(id=$id, name='$name', destination=${destination.code}, photoUrl='$photoUrl')"
    }

    class Builder {
        var id: Long? = null
        var name: String? = null
        var destination: Destination? = null
        var photoUrl: String? = null

        fun id(id: Long?) = apply {
            this.id = id
        }

        fun name(name: String) = apply {
            require(name.isNotBlank()) { "The hotel must have a name." }
            this.name = name
        }

        fun destination(destination: Destination) = apply {
            this.destination = destination
        }

        fun photoUrl(photoUrl: String) = apply {
            require(photoUrl.isNotBlank()) { "The hotel must have a photo url." }
            this.photoUrl = photoUrl
        }

        fun build(): Hotel {
            requireNotNull(name) { "The hotel must have a name." }
            requireNotNull(destination) { "The hotel must have a destination." }
            requireNotNull(photoUrl) { "The hotel must have a photo url." }
            return Hotel(this)
        }
    }
}
