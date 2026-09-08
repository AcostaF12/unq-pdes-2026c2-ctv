package unq.pdes.backend.service

import java.time.LocalDateTime
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    fun findMine(username: String): List<Purchase> {
        val buyer = userService.findByUsername(username)
        return purchaseRepository.findByBuyerIdOrderByPurchasedAtDesc(buyer.id!!)
    }

    @Transactional(readOnly = true)
    fun findForAgency(username: String): List<Purchase> {
        val agencyUser = userService.findByUsername(username) as? AgencyUser
            ?: throw AccessDeniedException("Only agency users can list agency purchases.")
        return purchaseRepository.findByAgencyIdOrderByPurchasedAtDesc(agencyUser.agency.id!!)
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
