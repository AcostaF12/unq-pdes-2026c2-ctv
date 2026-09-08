package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.Favorite
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.persistence.jpa.FavoriteRepository

@Service
class FavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val travelPackageService: TravelPackageService,
    private val userService: UserService,
) {

    @Transactional(readOnly = true)
    fun findMine(username: String): List<Favorite> {
        val buyer = userService.findByUsername(username)
        return favoriteRepository.findByBuyerId(buyer.id!!)
    }

    @Transactional
    fun add(username: String, packageId: Long): Favorite {
        val buyer = requireBuyer(username)
        val travelPackage = travelPackageService.findById(packageId)
        require(!favoriteRepository.existsByBuyerIdAndTravelPackageId(buyer.id!!, packageId)) {
            "The package is already in favorites."
        }
        return favoriteRepository.save(
            Favorite.Builder()
                .buyer(buyer)
                .travelPackage(travelPackage)
                .build(),
        )
    }

    @Transactional
    fun remove(username: String, packageId: Long) {
        val buyer = requireBuyer(username)
        val favorite = favoriteRepository.findByBuyerIdAndTravelPackageId(buyer.id!!, packageId)
            ?: throw EntityNotFoundException("The package is not in favorites.")
        favoriteRepository.delete(favorite)
    }

    private fun requireBuyer(username: String) = userService.findByUsername(username).also { buyer ->
        if (buyer.role != Role.BUYER) {
            throw AccessDeniedException("Only buyers can manage favorites.")
        }
    }
}
