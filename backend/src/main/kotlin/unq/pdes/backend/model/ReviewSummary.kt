package unq.pdes.backend.model

data class ReviewSummary(
    val packageId: Long,
    val averageScore: Double,
    val reviewCount: Long,
)
