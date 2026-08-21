package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Destination

data class DestinationDto(
    val code: String,
    val city: String,
) {
    companion object {
        fun fromModel(destination: Destination): DestinationDto {
            return DestinationDto(
                code = destination.code,
                city = destination.city,
            )
        }
    }

    fun toModel(): Destination {
        return Destination(code, city)
    }
}
