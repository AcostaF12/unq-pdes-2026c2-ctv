package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Agency

data class AgencyDto(
    val id: Long?,
    val name: String,
    val active: Boolean = true,
) {
    companion object {
        fun fromModel(agency: Agency): AgencyDto =
            AgencyDto(
                id = agency.id,
                name = agency.name,
                active = agency.active,
            )
    }
}
