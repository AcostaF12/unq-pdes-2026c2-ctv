package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.backend.model.Review

interface ReviewRepository : JpaRepository<Review, Long> {

    fun findByTravelPackageId(travelPackageId: Long): List<Review>

    fun existsByBuyerIdAndTravelPackageId(buyerId: Long, travelPackageId: Long): Boolean
}
