package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.backend.model.Purchase

interface PurchaseRepository : JpaRepository<Purchase, Long> {

    fun findByBuyerId(buyerId: Long): List<Purchase>
}
