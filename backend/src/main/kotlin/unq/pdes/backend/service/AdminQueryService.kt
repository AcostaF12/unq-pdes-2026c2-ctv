package unq.pdes.backend.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.Favorite
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.Review
import unq.pdes.backend.persistence.jpa.FavoriteRepository
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository

@Service
@Transactional(readOnly = true)
class AdminQueryService(
    private val favorites: FavoriteRepository,
    private val reviews: ReviewRepository,
    private val purchases: PurchaseRepository,
) {
    fun favorites(
        page: Int,
        size: Int,
    ): Page<Favorite> = favorites.findAll(pageRequest(page, size))

    fun reviews(
        page: Int,
        size: Int,
    ): Page<Review> = reviews.findAll(pageRequest(page, size))

    fun purchases(
        page: Int,
        size: Int,
    ): Page<Purchase> = purchases.findAll(pageRequest(page, size))

    private fun pageRequest(
        page: Int,
        size: Int,
        sort: Sort = Sort.by(Sort.Direction.DESC, "id"),
    ): PageRequest {
        require(page >= 0) { "Page must be zero or greater." }
        require(size in 1..100) { "Page size must be between 1 and 100." }
        return PageRequest.of(page, size, sort)
    }
}
