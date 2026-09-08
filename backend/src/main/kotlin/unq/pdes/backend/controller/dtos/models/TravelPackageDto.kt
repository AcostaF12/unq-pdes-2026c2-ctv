package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.TravelPackage
import java.math.BigDecimal

data class TravelPackageDto(
    val id: Long?,
    val name: String,
    val price: BigDecimal,
    val agency: AgencyDto,
    val hotel: HotelDto,
    val origin: CityDto,
    val destination: CityDto,
    val outboundFlightId: Long,
    val returnFlightId: Long,
) {
    companion object {
        fun fromModel(travelPackage: TravelPackage): TravelPackageDto {
            return TravelPackageDto(
                id = travelPackage.id,
                name = travelPackage.name,
                price = travelPackage.price,
                agency = AgencyDto.fromModel(travelPackage.agency),
                hotel = HotelDto.fromModel(travelPackage.hotel),
                origin = CityDto.fromModel(travelPackage.origin),
                destination = CityDto.fromModel(travelPackage.destination),
                outboundFlightId = travelPackage.outboundFlightId,
                returnFlightId = travelPackage.returnFlightId,
            )
        }
    }
}
