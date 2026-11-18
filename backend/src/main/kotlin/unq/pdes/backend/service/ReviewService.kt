package unq.pdes.backend.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.ReviewSummary
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val purchaseRepository: PurchaseRepository,
    private val travelPackageService: TravelPackageService,
    private val userService: UserService,
) {
    @Transactional(readOnly = true)
    fun findByPackageId(packageId: Long): List<Review> {
        travelPackageService.findById(packageId)
        return reviewRepository.findByTravelPackageId(packageId)
    }

    @Transactional(readOnly = true)
    fun findByBuyerAndPackage(
        buyerId: Long,
        packageId: Long,
    ): Review? = reviewRepository.findByBuyerIdAndTravelPackageId(buyerId, packageId)

    /** Average score and review count for every given package in a single grouped query. */
    @Transactional(readOnly = true)
    fun summarize(packageIds: Collection<Long>): Map<Long, ReviewSummary> =
        if (packageIds.isEmpty()) {
            emptyMap()
        } else {
            reviewRepository.summarizeByPackageIds(packageIds.toSet()).associateBy { it.packageId }
        }

    @Transactional
    fun create(
        username: String,
        packageId: Long,
        score: Int,
        comment: String?,
    ): Review {
        val buyer = userService.findByUsername(username)
        if (buyer.role != Role.BUYER) {
            throw AccessDeniedException("Only buyers can write reviews.")
        }
        val travelPackage = travelPackageService.findById(packageId)
        require(purchaseRepository.existsByBuyerIdAndTravelPackageId(buyer.id!!, packageId)) {
            "You can only review packages you purchased."
        }
        require(!reviewRepository.existsByBuyerIdAndTravelPackageId(buyer.id!!, packageId)) {
            "You already reviewed this package."
        }
        return reviewRepository.save(
            Review
                .Builder()
                .buyer(buyer)
                .travelPackage(travelPackage)
                .score(score)
                .comment(comment)
                .build(),
        )
    }
}
