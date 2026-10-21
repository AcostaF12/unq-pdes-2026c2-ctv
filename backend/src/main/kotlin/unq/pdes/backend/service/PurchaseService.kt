package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.search.PurchaseHistorySearchCriteria
import unq.pdes.backend.search.PurchaseHistorySearchSpecification
import unq.pdes.backend.search.PurchaseHistorySort
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PurchaseService(
    private val purchaseRepository: PurchaseRepository,
    private val travelPackageService: TravelPackageService,
    private val userService: UserService,
    private val flightsClient: FlightsClient,
) {
    @Transactional(readOnly = true)
    @Suppress("LongParameterList")
    fun findMine( // NOSONAR kotlin:S107, flat history filters
        username: String,
        page: Int = 0,
        size: Int = 20,
        packageName: String? = null,
        destination: String? = null,
        from: LocalDate? = null,
        to: LocalDate? = null,
        minPrice: BigDecimal? = null,
        maxPrice: BigDecimal? = null,
        sort: String? = null,
    ): Page<Purchase> {
        val buyer = userService.findByUsername(username)
        if (buyer.role != Role.BUYER) {
            throw AccessDeniedException("Only buyers can list their purchase history.")
        }
        validatePaging(page, size)
        val criteria = PurchaseHistorySearchCriteria.from(packageName, destination, from, to, minPrice, maxPrice)
        val pageable = PageRequest.of(page, size, PurchaseHistorySort.parse(sort))
        return purchaseRepository.findAll(PurchaseHistorySearchSpecification.forBuyer(buyer.id!!, criteria), pageable)
    }

    /** Returns the purchase when the viewer is its buyer or a user of the selling agency. */
    @Transactional(readOnly = true)
    fun findForViewer(
        username: String,
        id: Long,
    ): Purchase {
        val purchase =
            purchaseRepository
                .findById(id)
                .orElseThrow { EntityNotFoundException("There is no Purchase with id: $id.") }
        val viewer = userService.findByUsername(username)
        val isBuyer = purchase.buyer.id == viewer.id
        val isSellingAgency = (viewer as? AgencyUser)?.agency?.id == purchase.agency.id
        if (!isBuyer && !isSellingAgency) {
            throw AccessDeniedException("You cannot access this purchase.")
        }
        return purchase
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
        val agencyUser =
            userService.findByUsername(username) as? AgencyUser
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
    fun hasPurchased(
        username: String,
        packageId: Long,
    ): Boolean {
        val buyer = userService.findByUsername(username)
        if (buyer.role != Role.BUYER) throw AccessDeniedException("Only buyers can check their purchases.")
        return purchaseRepository.existsByBuyerIdAndTravelPackageId(buyer.id!!, packageId)
    }

    private fun pageRequest(
        page: Int,
        size: Int,
    ): PageRequest {
        validatePaging(page, size)
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("purchasedAt"), Sort.Order.desc("id")))
    }

    private fun validatePaging(
        page: Int,
        size: Int,
    ) {
        require(page >= 0) { "Page must be zero or greater." }
        require(size in 1..100) { "Page size must be between 1 and 100." }
    }

    @Transactional
    fun purchase(
        username: String,
        packageId: Long,
    ): Purchase {
        val buyer = userService.findByUsername(username)
        if (buyer.role != Role.BUYER) {
            throw AccessDeniedException("Only buyers can purchase packages.")
        }
        val travelPackage = travelPackageService.findById(packageId)
        val passengerName = "${buyer.firstName} ${buyer.lastName}"

        val outboundSale = flightsClient.sell(travelPackage.outboundFlightId, passengerName)
        val returnSale =
            try {
                flightsClient.sell(travelPackage.returnFlightId, passengerName)
            } catch (ex: Exception) {
                outboundSale.id?.let { runCatching { flightsClient.cancelSale(it) } }
                throw ex
            }

        return try {
            purchaseRepository.save(
                Purchase
                    .Builder()
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
