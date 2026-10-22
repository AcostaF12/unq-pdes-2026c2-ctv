package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.Role
import unq.pdes.backend.model.user.User
import unq.pdes.backend.persistence.jpa.FavoriteRepository
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.persistence.jpa.TravelPackageRepository
import unq.pdes.backend.search.TravelPackageSearchCriteria
import unq.pdes.backend.search.TravelPackageSearchMetrics
import unq.pdes.backend.search.TravelPackageSearchPageable
import unq.pdes.backend.search.TravelPackageSearchSpecification
import java.math.BigDecimal
import java.time.Duration

@Service
class TravelPackageService(
    private val travelPackageRepository: TravelPackageRepository,
    private val hotelService: HotelService,
    private val cityService: CityService,
    private val userService: UserService,
    private val agencyService: AgencyService,
    private val flightsClient: FlightsClient,
    private val purchaseRepository: PurchaseRepository,
    private val favoriteRepository: FavoriteRepository,
    private val reviewRepository: ReviewRepository,
    private val searchMetrics: TravelPackageSearchMetrics,
) {
    @Transactional(readOnly = true)
    fun search(
        criteria: TravelPackageSearchCriteria,
        pageable: Pageable,
        active: Boolean? = true,
        includeInactiveAgencies: Boolean = false,
    ): Page<TravelPackage> {
        val sanitizedPageable = TravelPackageSearchPageable.sanitize(pageable)
        val specification =
            TravelPackageSearchSpecification.matching(
                criteria,
                TravelPackageSearchPageable.ratingDirection(sanitizedPageable),
                active,
                includeInactiveAgencies,
            )
        val startedAt = System.nanoTime()
        val result = travelPackageRepository.findAll(specification, TravelPackageSearchPageable.withoutRating(sanitizedPageable))
        searchMetrics.record(criteria, result.totalElements, Duration.ofNanos(System.nanoTime() - startedAt))
        return result
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): TravelPackage =
        travelPackageRepository
            .findById(id)
            .orElseThrow { EntityNotFoundException("There is no TravelPackage with id: $id.") }

    @Transactional(readOnly = true)
    fun findMine(username: String): List<TravelPackage> {
        val agencyUser = requireAgencyUser(username)
        return travelPackageRepository.findByAgencyId(agencyUser.agency.id!!)
    }

    @Transactional
    fun findForUpdate(id: Long): TravelPackage =
        travelPackageRepository.findForUpdate(id)
            ?: throw EntityNotFoundException("There is no TravelPackage with id: $id.")

    @Transactional
    fun setActive(
        username: String,
        id: Long,
        active: Boolean,
    ): TravelPackage {
        val manager = requireManager(username)
        val existing = findForUpdate(id)
        requireCanModify(manager, existing)
        if (active) {
            val outbound = flightsClient.findById(existing.outboundFlightId)
            val inbound = flightsClient.findById(existing.returnFlightId)
            require(!outbound.flightDate.isBefore(java.time.LocalDate.now())) { "No se puede publicar un paquete con vuelos vencidos." }
            require(outbound.availability > 0 && inbound.availability > 0) { "Los vuelos deben tener lugares disponibles." }
        }
        existing.active = active
        return travelPackageRepository.save(existing)
    }

    @Transactional
    fun create(
        username: String,
        name: String,
        hotelId: Long,
        outboundFlightId: Long,
        returnFlightId: Long,
        price: BigDecimal,
        agencyId: Long? = null,
    ): TravelPackage {
        val agency = resolveAgency(requireManager(username), agencyId)
        requireUniqueName(agency.id!!, name, null)
        val hotel = hotelService.findById(hotelId)
        val (origin, destination) = resolveCities(hotel, outboundFlightId, returnFlightId)
        val travelPackage =
            TravelPackage
                .Builder()
                .agency(agency)
                .hotel(hotel)
                .origin(origin)
                .destination(destination)
                .name(name)
                .outboundFlightId(outboundFlightId)
                .returnFlightId(returnFlightId)
                .price(price)
                .build()
        return travelPackageRepository.save(travelPackage)
    }

    @Transactional
    fun update( // NOSONAR kotlin:S107, mirrors the package form fields
        username: String,
        id: Long,
        name: String,
        hotelId: Long,
        outboundFlightId: Long,
        returnFlightId: Long,
        price: BigDecimal,
        agencyId: Long? = null,
    ): TravelPackage {
        val manager = requireManager(username)
        val existing = findForUpdate(id)
        requireCanModify(manager, existing)
        val agency = resolveAgency(manager, agencyId)
        require(agency.id == existing.agency.id || !purchaseRepository.existsByTravelPackageId(id)) {
            "Cannot move a package that already has purchases to another agency."
        }
        requireUniqueName(agency.id!!, name, id)
        val hotel = hotelService.findById(hotelId)
        val (origin, destination) = resolveCities(hotel, outboundFlightId, returnFlightId)
        val travelPackage =
            TravelPackage
                .Builder()
                .id(id)
                .active(existing.active)
                .agency(agency)
                .hotel(hotel)
                .origin(origin)
                .destination(destination)
                .name(name)
                .outboundFlightId(outboundFlightId)
                .returnFlightId(returnFlightId)
                .price(price)
                .build()
        return travelPackageRepository.save(travelPackage)
    }

    @Transactional
    fun deleteById(
        username: String,
        id: Long,
    ) {
        val travelPackage = findForUpdate(id)
        requireCanModify(requireManager(username), travelPackage)
        require(!purchaseRepository.existsByTravelPackageId(id)) {
            "Cannot delete a package that already has purchases."
        }
        require(!favoriteRepository.existsByTravelPackageId(id)) {
            "Cannot delete a package that is marked as favorite."
        }
        require(!reviewRepository.existsByTravelPackageId(id)) {
            "Cannot delete a package that already has reviews."
        }
        travelPackageRepository.delete(travelPackage)
    }

    private fun resolveCities(
        hotel: Hotel,
        outboundFlightId: Long,
        returnFlightId: Long,
    ): Pair<City, City> {
        val outbound = flightsClient.findById(outboundFlightId)
        val returnFlight = flightsClient.findById(returnFlightId)
        require(returnFlight.origin == outbound.destination) {
            "The return flight origin must match the outbound destination."
        }
        require(returnFlight.destination == outbound.origin) {
            "The return flight destination must match the outbound origin."
        }
        require(hotel.city.code == outbound.destination) {
            "The hotel must be located in the destination city."
        }
        return cityService.findByCode(outbound.origin) to
            cityService.findByCode(outbound.destination)
    }

    private fun requireUniqueName(
        agencyId: Long,
        name: String,
        currentId: Long?,
    ) {
        val taken =
            if (currentId == null) {
                travelPackageRepository.existsByAgencyIdAndName(agencyId, name)
            } else {
                travelPackageRepository.existsByAgencyIdAndNameAndIdNot(agencyId, name, currentId)
            }
        require(!taken) { "The agency already has a package named '$name'." }
    }

    private fun requireCanModify(
        manager: User,
        travelPackage: TravelPackage,
    ) {
        if (manager is AgencyUser && manager.agency.id != travelPackage.agency.id) {
            throw AccessDeniedException("Only the owning agency can modify this package.")
        }
    }

    private fun resolveAgency(
        manager: User,
        agencyId: Long?,
    ): Agency =
        (manager as? AgencyUser)?.agency
            ?: agencyService.findById(requireNotNull(agencyId) { "An agency is required to manage packages as admin." })

    private fun requireManager(username: String): User {
        val user = userService.findByUsername(username)
        if (user is AgencyUser || user.role == Role.ADMIN) {
            return user
        }
        throw AccessDeniedException("Only agency or admin users can manage packages.")
    }

    private fun requireAgencyUser(username: String): AgencyUser {
        val user = userService.findByUsername(username)
        return user as? AgencyUser
            ?: throw AccessDeniedException("Only agency users can manage packages.")
    }
}
