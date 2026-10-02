package unq.pdes.backend.persistence.jpa

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import unq.pdes.backend.model.Purchase
import java.time.LocalDateTime

interface PurchaseRepository : JpaRepository<Purchase, Long> {
    fun findByBuyerId(
        buyerId: Long,
        pageable: Pageable,
    ): Page<Purchase>

    @Query(
        """SELECT p FROM Purchase p
            WHERE p.agency.id = :agencyId
              AND (:buyerUsername IS NULL OR p.buyerSnapshot.username = :buyerUsername)
              AND (:packageName IS NULL OR LOWER(p.travelSnapshot.name) LIKE LOWER(CONCAT('%', :packageName, '%')))
              AND (:fromInclusive IS NULL OR p.purchasedAt >= :fromInclusive)
              AND (:toExclusive IS NULL OR p.purchasedAt < :toExclusive)""",
    )
    fun searchAgencyHistory(
        @Param("agencyId") agencyId: Long,
        @Param("buyerUsername") buyerUsername: String?,
        @Param("packageName") packageName: String?,
        @Param("fromInclusive") fromInclusive: LocalDateTime?,
        @Param("toExclusive") toExclusive: LocalDateTime?,
        pageable: Pageable,
    ): Page<Purchase>

    fun existsByBuyerIdAndTravelPackageId(
        buyerId: Long,
        travelPackageId: Long,
    ): Boolean

    fun existsByTravelPackageId(travelPackageId: Long): Boolean
}
