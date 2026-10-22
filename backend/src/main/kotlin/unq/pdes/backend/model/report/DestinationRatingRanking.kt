package unq.pdes.backend.model.report

data class DestinationRatingRanking(
    val code: String,
    val name: String,
    val average: Double,
    val reviewCount: Long,
)
