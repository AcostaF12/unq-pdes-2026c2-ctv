package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.City

data class CityDto(
    val code: String,
    val name: String,
) {
    companion object {
        fun fromModel(city: City): CityDto =
            CityDto(
                code = city.code,
                name = city.name,
            )
    }

    fun toModel(): City = City(code, name)
}
