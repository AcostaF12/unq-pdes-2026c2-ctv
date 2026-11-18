package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.ReviewSummary

interface ReviewRepository : JpaRepository<Review, Long> {
    fun findByTravelPackageId(travelPackageId: Long): List<Review>

    fun findByBuyerIdAndTravelPackageId(
        buyerId: Long,
        travelPackageId: Long,
    ): Review?

    fun existsByBuyerIdAndTravelPackageId(
        buyerId: Long,
        travelPackageId: Long,
    ): Boolean

    fun existsByTravelPackageId(travelPackageId: Long): Boolean

    @Query(
        """SELECT new unq.pdes.backend.model.ReviewSummary(r.travelPackage.id, AVG(r.score), COUNT(r))
            FROM Review r
            WHERE r.travelPackage.id IN :packageIds
            GROUP BY r.travelPackage.id""",
    )
    fun summarizeByPackageIds(
        @Param("packageIds") packageIds: Collection<Long>,
    ): List<ReviewSummary>
}
