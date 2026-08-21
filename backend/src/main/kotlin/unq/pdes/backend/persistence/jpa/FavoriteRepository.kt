package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.backend.model.Favorite

interface FavoriteRepository : JpaRepository<Favorite, Long> {

    fun findByBuyerId(buyerId: Long): List<Favorite>

    fun existsByBuyerIdAndTravelPackageId(buyerId: Long, travelPackageId: Long): Boolean
}
