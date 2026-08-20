package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Hotel

data class HotelDto(
    val id: Long?,
    val name: String,
    val destination: DestinationDto,
    val photoUrl: String,
) {
    companion object {
        fun fromModel(hotel: Hotel): HotelDto {
            return HotelDto(
                id = hotel.id,
                name = hotel.name,
                destination = DestinationDto.fromModel(hotel.destination),
                photoUrl = hotel.photoUrl,
            )
        }
    }

    fun toModel(): Hotel {
        return Hotel.Builder()
            .id(id)
            .name(name)
            .destination(destination.toModel())
            .photoUrl(photoUrl)
            .build()
    }
}
