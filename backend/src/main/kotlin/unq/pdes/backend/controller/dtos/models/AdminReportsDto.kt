package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.report.CountRanking
import unq.pdes.backend.model.report.DestinationCountRanking
import unq.pdes.backend.model.report.DestinationRatingRanking

data class AdminReportsDto(
    val topBuyers: List<CountRanking>,
    val topAgencies: List<CountRanking>,
    val topDestinations: List<DestinationCountRanking>,
    val topRatedDestinations: List<DestinationRatingRanking>,
)
