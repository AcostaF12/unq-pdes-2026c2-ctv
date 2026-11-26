package unq.pdes.backend.service

import jakarta.annotation.PreDestroy
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import unq.pdes.backend.external.flights.ExternalFlightSaleDto
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.PurchaseStatus
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.search.PurchaseHistorySearchCriteria
import unq.pdes.backend.search.PurchaseHistorySearchSpecification
import unq.pdes.backend.search.PurchaseHistorySort
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Service
class PurchaseService(
    private val purchaseRepository: PurchaseRepository,
    private val travelPackageService: TravelPackageService,
    private val userService: UserService,
    private val flightsClient: FlightsClient,
    private val entityManager: EntityManager,
    transactionManager: PlatformTransactionManager,
) {
    private val transactions = TransactionTemplate(transactionManager)

    private companion object {
        const val FLIGHT_CALL_THREADS = 16
    }

    // Ida y vuelta pegan a vuelos distintos: se pueden hacer a la vez. El pool acota los hilos.
    private val flightCalls: ExecutorService =
        Executors.newFixedThreadPool(FLIGHT_CALL_THREADS) { runnable ->
            Thread(runnable, "flight-call").apply { isDaemon = true }
        }

    @PreDestroy
    fun shutdownFlightCalls() {
        flightCalls.shutdown()
    }

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
        if (!isBuyer && !isSellingAgency && viewer.role != Role.ADMIN) {
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
    fun findForAdmin(
        username: String,
        page: Int,
        size: Int,
        buyerUsername: String?,
        packageName: String?,
        from: LocalDate?,
        to: LocalDate?,
    ): Page<Purchase> {
        if (userService.findByUsername(username).role != Role.ADMIN) {
            throw AccessDeniedException("Only admins can list all sales.")
        }
        require(from == null || to == null || !from.isAfter(to)) { "The start date must not be after the end date." }
        return purchaseRepository.searchAgencyHistory(
            agencyId = null,
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
        return purchaseRepository.existsByBuyerIdAndTravelPackageIdAndStatus(buyer.id!!, packageId, PurchaseStatus.ACTIVE)
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

    /**
     * Sells both seats before taking the package row lock. The lock only covers the re-check and the
     * insert, so concurrent buyers of a package with seats left do not queue on the flight HTTP.
     * Oversell is still prevented by the flight row lock in flights-service. If the package was archived
     * or its flights changed while the seats were being sold, both sales are cancelled.
     */
    fun purchase(
        username: String,
        packageId: Long,
    ): Purchase {
        val buyer = userService.findByUsername(username)
        if (buyer.role != Role.BUYER) {
            throw AccessDeniedException("Only buyers can purchase packages.")
        }
        val preview = travelPackageService.findById(packageId)
        require(preview.agency.active) { "This agency is inactive. The package is not available for purchase." }
        require(preview.active) { "Este paquete está archivado y no admite nuevas compras." }
        val outboundFlightId = preview.outboundFlightId
        val returnFlightId = preview.returnFlightId
        val passengerName = "${buyer.firstName} ${buyer.lastName}"

        val (outboundSale, returnSale) = sellSeats(outboundFlightId, returnFlightId, passengerName)

        return try {
            transactions.execute<Purchase> {
                val travelPackage = lockedPackage(preview)
                require(travelPackage.agency.active) { "This agency is inactive. The package is not available for purchase." }
                require(travelPackage.active) { "Este paquete está archivado y no admite nuevas compras." }
                require(travelPackage.outboundFlightId == outboundFlightId && travelPackage.returnFlightId == returnFlightId) {
                    "El paquete cambió de vuelos durante la compra."
                }
                val currentBuyer = if (entityManager.contains(buyer)) buyer else userService.findByUsername(username)
                purchaseRepository.save(
                    Purchase
                        .Builder()
                        .buyer(currentBuyer)
                        .travelPackage(travelPackage)
                        .agency(travelPackage.agency)
                        .purchasePrice(travelPackage.price)
                        .purchasedAt(LocalDateTime.now())
                        .flightSales(outboundSale.id, returnSale.id)
                        .build(),
                )
            }
        } catch (ex: Exception) {
            compensate(outboundSale.id, returnSale.id)
            throw ex
        }
    }

    /**
     * Logical delete. The flight calls happen before the purchase row is locked: releasing a sale is
     * idempotent (an already released sale answers 404), and only one transaction can mark the row
     * CANCELLED. A retry is safe when the seats were released but the local update did not commit.
     */
    fun cancel(
        username: String,
        id: Long,
    ): Purchase {
        val purchase =
            purchaseRepository.findById(id).orElseThrow {
                EntityNotFoundException("There is no Purchase with id: $id.")
            }
        val requester = userService.findByUsername(username)
        if (purchase.buyer.id != requester.id && requester.role != Role.ADMIN) {
            throw AccessDeniedException("Only the buyer or an admin can cancel this purchase.")
        }
        require(purchase.isActive) { "La compra ya fue cancelada." }
        val outbound = flightsClient.findById(purchase.travelSnapshot.outboundFlightId)
        val now = LocalDateTime.now()
        require(LocalDateTime.of(outbound.flightDate, outbound.departureTime).isAfter(now)) {
            "El vuelo de ida ya partió. La compra no puede cancelarse."
        }
        releaseSeats(listOfNotNull(purchase.outboundSaleId, purchase.returnSaleId))
        return transactions.execute<Purchase> {
            val locked = lockedPurchase(purchase, id)
            require(locked.isActive) { "La compra ya fue cancelada." }
            locked.cancel(now)
            purchaseRepository.save(locked)
        }
    }

    private fun sellSeats(
        outboundFlightId: Long,
        returnFlightId: Long,
        passengerName: String,
    ): Pair<ExternalFlightSaleDto, ExternalFlightSaleDto> {
        val outboundFuture = inFlightContext { flightsClient.sell(outboundFlightId, passengerName) }
        val returnFuture = inFlightContext { flightsClient.sell(returnFlightId, passengerName) }
        val outbound = unwrap(runCatching { outboundFuture.join() })
        val returning = unwrap(runCatching { returnFuture.join() })
        if (outbound.isFailure || returning.isFailure) {
            compensate(outbound.getOrNull()?.id, returning.getOrNull()?.id)
            throw outbound.exceptionOrNull() ?: returning.exceptionOrNull()!!
        }
        return outbound.getOrThrow() to returning.getOrThrow()
    }

    /**
     * One locked read. If OSIV already holds the row, refresh re-reads it under the lock.
     * A second SELECT FOR UPDATE would only keep the row locked for longer.
     */
    private fun lockedPackage(preview: TravelPackage): TravelPackage {
        if (entityManager.contains(preview)) {
            refreshOrMissing(preview, "There is no TravelPackage with id: ${preview.id}.")
            return preview
        }
        return travelPackageService.findForUpdate(preview.id!!)
    }

    private fun lockedPurchase(
        purchase: Purchase,
        id: Long,
    ): Purchase {
        if (entityManager.contains(purchase)) {
            refreshOrMissing(purchase, "There is no Purchase with id: $id.")
            return purchase
        }
        return purchaseRepository.findForUpdate(id)
            ?: throw EntityNotFoundException("There is no Purchase with id: $id.")
    }

    private fun refreshOrMissing(
        entity: Any,
        missingMessage: String,
    ) {
        try {
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE)
        } catch (_: EntityNotFoundException) {
            throw EntityNotFoundException(missingMessage)
        }
    }

    /** Both compensations run together. A failure here must not replace the original error. */
    private fun compensate(
        firstSaleId: Long?,
        secondSaleId: Long?,
    ) {
        val ids = listOfNotNull(firstSaleId, secondSaleId)
        if (ids.size < 2) {
            ids.forEach { id -> runCatching { flightsClient.cancelSale(id) } }
            return
        }
        val first = CompletableFuture.runAsync({ runCatching { flightsClient.cancelSale(ids[0]) } }, flightCalls)
        val second = CompletableFuture.runAsync({ runCatching { flightsClient.cancelSale(ids[1]) } }, flightCalls)
        first.join()
        second.join()
    }

    private fun <T> inFlightContext(block: () -> T): CompletableFuture<T> = CompletableFuture.supplyAsync(block, flightCalls)

    private fun <T> unwrap(result: Result<T>): Result<T> =
        result.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> Result.failure((error as? CompletionException)?.cause ?: error) },
        )

    private fun releaseSeats(saleIds: List<Long>) {
        if (saleIds.size < 2) {
            saleIds.forEach(::releaseSeat)
            return
        }
        val first = CompletableFuture.runAsync({ releaseSeat(saleIds[0]) }, flightCalls)
        val second = CompletableFuture.runAsync({ releaseSeat(saleIds[1]) }, flightCalls)
        val firstError = runCatching { first.join() }.exceptionOrNull()
        val secondError = runCatching { second.join() }.exceptionOrNull()
        val error = firstError ?: secondError ?: return
        throw (error as? CompletionException)?.cause ?: error
    }

    private fun releaseSeat(saleId: Long) {
        try {
            flightsClient.cancelSale(saleId)
        } catch (_: EntityNotFoundException) {
            // Already released by a previous, partially failed attempt: nothing left to give back.
        }
    }
}
