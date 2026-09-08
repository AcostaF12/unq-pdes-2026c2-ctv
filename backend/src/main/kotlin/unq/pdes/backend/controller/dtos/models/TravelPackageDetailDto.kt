package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.external.flights.ExternalFlightDto
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import java.math.BigDecimal

data class TravelPackageDetailDto(
    val id: Long?,
    val name: String,
    val price: BigDecimal,
    val agency: AgencyDto,
    val hotel: HotelDto,
    val origin: CityDto,
    val destination: CityDto,
    val outboundFlightId: Long,
    val returnFlightId: Long,
    val outboundFlight: FlightDto?,
    val returnFlight: FlightDto?,
    val reviews: List<ReviewDto>,
    val averageScore: Double?,
) {
    companion object {
        fun from(
            travelPackage: TravelPackage,
            outboundFlight: ExternalFlightDto?,
            returnFlight: ExternalFlightDto?,
            reviews: List<Review>,
        ): TravelPackageDetailDto {
            return TravelPackageDetailDto(
                id = travelPackage.id,
                name = travelPackage.name,
                price = travelPackage.price,
                agency = AgencyDto.fromModel(travelPackage.agency),
                hotel = HotelDto.fromModel(travelPackage.hotel),
                origin = CityDto.fromModel(travelPackage.origin),
                destination = CityDto.fromModel(travelPackage.destination),
                outboundFlightId = travelPackage.outboundFlightId,
                returnFlightId = travelPackage.returnFlightId,
                outboundFlight = outboundFlight?.let { FlightDto.fromExternal(it) },
                returnFlight = returnFlight?.let { FlightDto.fromExternal(it) },
                reviews = reviews.map { ReviewDto.fromModel(it) },
                averageScore = reviews.takeIf { it.isNotEmpty() }?.map { it.score }?.average(),
            )
        }
    }
}
