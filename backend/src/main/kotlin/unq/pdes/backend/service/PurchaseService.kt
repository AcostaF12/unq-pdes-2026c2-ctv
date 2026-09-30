package unq.pdes.backend.service

import java.time.LocalDateTime
import java.time.LocalDate
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.persistence.jpa.PurchaseRepository

@Service
class PurchaseService(
    private val purchaseRepository: PurchaseRepository,
    private val travelPackageService: TravelPackageService,
    private val userService: UserService,
    private val flightsClient: FlightsClient,
) {

    @Transactional(readOnly = true)
    fun findMine(username: String, page: Int = 0, size: Int = 20): Page<Purchase> {
        val buyer = userService.findByUsername(username)
        if (buyer.role != Role.BUYER) {
            throw AccessDeniedException("Only buyers can list their purchase history.")
        }
        return purchaseRepository.findByBuyerId(buyer.id!!, pageRequest(page, size))
    }

    @Transactional(readOnly = true)
    fun findForAgency(
        username: String,
        page: Int = 0,
        size: Int = 20,
        buyerUsername: String? = null,
        packageName: String? = null,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): Page<Purchase> {
        val agencyUser = userService.findByUsername(username) as? AgencyUser
            ?: throw AccessDeniedException("Only agency users can list agency purchases.")
        require(from == null || to == null || !from.isAfter(to)) { "The start date must not be after the end date." }
        return purchaseRepository.searchAgencyHistory(
            agencyId = agencyUser.agency.id!!,
            buyerUsername = buyerUsername?.trim()?.takeIf(String::isNotEmpty),
            packageName = packageName?.trim()?.takeIf(String::isNotEmpty),
            fromInclusive = from?.atStartOfDay(),
            toExclusive = to?.plusDays(1)?.atStartOfDay(),
            pageable = pageRequest(page, size),
        )
    }

    @Transactional(readOnly = true)
    fun hasPurchased(username: String, packageId: Long): Boolean {
        val buyer = userService.findByUsername(username)
        if (buyer.role != Role.BUYER) throw AccessDeniedException("Only buyers can check their purchases.")
        return purchaseRepository.existsByBuyerIdAndTravelPackageId(buyer.id!!, packageId)
    }

    private fun pageRequest(page: Int, size: Int): PageRequest {
        require(page >= 0) { "Page must be zero or greater." }
        require(size in 1..100) { "Page size must be between 1 and 100." }
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("purchasedAt"), Sort.Order.desc("id")))
    }

    @Transactional
    fun purchase(username: String, packageId: Long): Purchase {
        val buyer = userService.findByUsername(username)
        if (buyer.role != Role.BUYER) {
            throw AccessDeniedException("Only buyers can purchase packages.")
        }
        val travelPackage = travelPackageService.findById(packageId)
        val passengerName = "${buyer.firstName} ${buyer.lastName}"

        val outboundSale = flightsClient.sell(travelPackage.outboundFlightId, passengerName)
        val returnSale = try {
            flightsClient.sell(travelPackage.returnFlightId, passengerName)
        } catch (ex: Exception) {
            outboundSale.id?.let { runCatching { flightsClient.cancelSale(it) } }
            throw ex
        }

        return try {
            purchaseRepository.save(
                Purchase.Builder()
                    .buyer(buyer)
                    .travelPackage(travelPackage)
                    .agency(travelPackage.agency)
                    .purchasePrice(travelPackage.price)
                    .purchasedAt(LocalDateTime.now())
                    .build(),
            )
        } catch (ex: Exception) {
            outboundSale.id?.let { runCatching { flightsClient.cancelSale(it) } }
            returnSale.id?.let { runCatching { flightsClient.cancelSale(it) } }
            throw ex
        }
    }
}
