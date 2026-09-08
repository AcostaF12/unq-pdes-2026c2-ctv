package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Hotel

data class HotelDto(
    val id: Long?,
    val name: String,
    val city: CityDto,
    val photoUrl: String,
) {
    companion object {
        fun fromModel(hotel: Hotel): HotelDto {
            return HotelDto(
                id = hotel.id,
                name = hotel.name,
                city = CityDto.fromModel(hotel.city),
                photoUrl = hotel.photoUrl,
            )
        }
    }

    fun toModel(): Hotel {
        return Hotel.Builder()
            .id(id)
            .name(name)
            .city(city.toModel())
            .photoUrl(photoUrl)
            .build()
    }
}
