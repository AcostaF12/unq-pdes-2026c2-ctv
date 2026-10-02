package unq.pdes.backend.model.history

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import unq.pdes.backend.model.TravelPackage
import java.math.BigDecimal

@Embeddable
class PurchaseTravelSnapshot private constructor(
    travelPackage: TravelPackage,
) {
    @Column(name = "package_snapshot_id", nullable = false)
    var id: Long = travelPackage.id!!

    @Column(name = "package_snapshot_name", nullable = false, length = 160)
    var name: String = travelPackage.name

    @Column(name = "package_snapshot_price", nullable = false, precision = 12, scale = 2)
    var price: BigDecimal = travelPackage.price

    @Column(name = "agency_snapshot_id", nullable = false)
    var agencyId: Long = travelPackage.agency.id!!

    @Column(name = "agency_snapshot_name", nullable = false, length = 160)
    var agencyName: String = travelPackage.agency.name

    @Column(name = "hotel_snapshot_id", nullable = false)
    var hotelId: Long = travelPackage.hotel.id!!

    @Column(name = "hotel_snapshot_name", nullable = false, length = 160)
    var hotelName: String = travelPackage.hotel.name

    @Column(name = "hotel_snapshot_city_code", nullable = false, length = 3)
    var hotelCityCode: String = travelPackage.hotel.city.code

    @Column(name = "hotel_snapshot_city_name", nullable = false, length = 100)
    var hotelCityName: String = travelPackage.hotel.city.name

    @Column(name = "hotel_snapshot_photo_url", nullable = false, length = 500)
    var hotelPhotoUrl: String = travelPackage.hotel.photoUrl

    @Column(name = "origin_snapshot_code", nullable = false, length = 3)
    var originCode: String = travelPackage.origin.code

    @Column(name = "origin_snapshot_name", nullable = false, length = 100)
    var originName: String = travelPackage.origin.name

    @Column(name = "destination_snapshot_code", nullable = false, length = 3)
    var destinationCode: String = travelPackage.destination.code

    @Column(name = "destination_snapshot_name", nullable = false, length = 100)
    var destinationName: String = travelPackage.destination.name

    @Column(name = "outbound_flight_snapshot_id", nullable = false)
    var outboundFlightId: Long = travelPackage.outboundFlightId

    @Column(name = "return_flight_snapshot_id", nullable = false)
    var returnFlightId: Long = travelPackage.returnFlightId

    companion object {
        fun from(travelPackage: TravelPackage): PurchaseTravelSnapshot = PurchaseTravelSnapshot(travelPackage)
    }
}
