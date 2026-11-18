package unq.pdes.backend.persistence.jpa

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.PurchaseStatus
import java.time.LocalDateTime

interface PurchaseRepository :
    JpaRepository<Purchase, Long>,
    JpaSpecificationExecutor<Purchase> {
    fun findByBuyerId(
        buyerId: Long,
        pageable: Pageable,
    ): Page<Purchase>

    @Query(
        """SELECT p FROM Purchase p
            WHERE (:agencyId IS NULL OR p.agency.id = :agencyId)
              AND (:buyerUsername IS NULL OR p.buyerSnapshot.username = :buyerUsername)
              AND (:packageName IS NULL OR LOWER(p.travelSnapshot.name) LIKE LOWER(CONCAT('%', CAST(:packageName AS string), '%')))
              AND (CAST(:fromInclusive AS LocalDateTime) IS NULL OR p.purchasedAt >= :fromInclusive)
              AND (CAST(:toExclusive AS LocalDateTime) IS NULL OR p.purchasedAt < :toExclusive)""",
    )
    fun searchAgencyHistory(
        @Param("agencyId") agencyId: Long?,
        @Param("buyerUsername") buyerUsername: String?,
        @Param("packageName") packageName: String?,
        @Param("fromInclusive") fromInclusive: LocalDateTime?,
        @Param("toExclusive") toExclusive: LocalDateTime?,
        pageable: Pageable,
    ): Page<Purchase>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Purchase p where p.id = :id")
    fun findForUpdate(
        @Param("id") id: Long,
    ): Purchase?

    fun existsByBuyerIdAndTravelPackageIdAndStatus(
        buyerId: Long,
        travelPackageId: Long,
        status: PurchaseStatus,
    ): Boolean

    fun existsByTravelPackageId(travelPackageId: Long): Boolean
}
