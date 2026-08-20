package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Agency

data class AgencyDto(
    val id: Long?,
    val name: String,
) {
    companion object {
        fun fromModel(agency: Agency): AgencyDto {
            return AgencyDto(
                id = agency.id,
                name = agency.name,
            )
        }
    }
}
