package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.history.PurchaseBuyerSnapshot
import unq.pdes.backend.model.history.PurchaseTravelSnapshot
import java.math.BigDecimal
import java.time.LocalDateTime

data class PurchaseDto(
    val id: Long?,
    val buyer: UserDto,
    val travelPackage: TravelPackageDto,
    val purchasePrice: BigDecimal,
    val purchasedAt: LocalDateTime,
) {
    companion object {
        fun fromModel(purchase: Purchase): PurchaseDto =
            PurchaseDto(
                id = purchase.id,
                buyer = buyerDto(purchase.buyerSnapshot),
                travelPackage = travelPackageDto(purchase.travelSnapshot),
                purchasePrice = purchase.purchasePrice,
                purchasedAt = purchase.purchasedAt,
            )

        private fun buyerDto(snapshot: PurchaseBuyerSnapshot): UserDto =
            UserDto(
                id = snapshot.id,
                username = snapshot.username,
                role = snapshot.role,
                firstName = snapshot.firstName,
                lastName = snapshot.lastName,
            )

        private fun travelPackageDto(snapshot: PurchaseTravelSnapshot): TravelPackageDto =
            TravelPackageDto(
                id = snapshot.id,
                name = snapshot.name,
                price = snapshot.price,
                agency = AgencyDto(snapshot.agencyId, snapshot.agencyName),
                hotel =
                    HotelDto(
                        id = snapshot.hotelId,
                        name = snapshot.hotelName,
                        city = CityDto(snapshot.hotelCityCode, snapshot.hotelCityName),
                        photoUrl = snapshot.hotelPhotoUrl,
                    ),
                origin = CityDto(snapshot.originCode, snapshot.originName),
                destination = CityDto(snapshot.destinationCode, snapshot.destinationName),
                outboundFlightId = snapshot.outboundFlightId,
                returnFlightId = snapshot.returnFlightId,
            )
    }
}
