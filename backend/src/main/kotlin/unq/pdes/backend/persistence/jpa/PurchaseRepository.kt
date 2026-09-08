package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.backend.model.Purchase

interface PurchaseRepository : JpaRepository<Purchase, Long> {

    fun findByBuyerIdOrderByPurchasedAtDesc(buyerId: Long): List<Purchase>

    fun findByAgencyIdOrderByPurchasedAtDesc(agencyId: Long): List<Purchase>

    fun existsByBuyerIdAndTravelPackageId(buyerId: Long, travelPackageId: Long): Boolean

    fun existsByTravelPackageId(travelPackageId: Long): Boolean
}
