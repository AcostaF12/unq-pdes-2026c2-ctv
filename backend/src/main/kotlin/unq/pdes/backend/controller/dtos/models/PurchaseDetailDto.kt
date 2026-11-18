package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.Review
import java.math.BigDecimal
import java.time.LocalDateTime

data class PurchaseDetailDto(
    val id: Long?,
    val buyer: UserDto,
    val travelPackage: TravelPackageDto,
    val purchasePrice: BigDecimal,
    val purchasedAt: LocalDateTime,
    val outboundFlight: FlightDto?,
    val returnFlight: FlightDto?,
    val review: ReviewDto?,
) {
    companion object {
        fun from(
            purchase: Purchase,
            outboundFlight: ExternalFlightDto?,
            returnFlight: ExternalFlightDto?,
            review: Review?,
        ): PurchaseDetailDto {
            val summary = PurchaseDto.fromModel(purchase)
            return PurchaseDetailDto(
                id = summary.id,
                buyer = summary.buyer,
                travelPackage = summary.travelPackage,
                purchasePrice = summary.purchasePrice,
                purchasedAt = summary.purchasedAt,
                outboundFlight = outboundFlight?.let { FlightDto.fromExternal(it) },
                returnFlight = returnFlight?.let { FlightDto.fromExternal(it) },
                review = review?.let { ReviewDto.fromModel(it) },
            )
        }
    }
}
