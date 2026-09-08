package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import java.math.BigDecimal
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.external.flights.FlightsClient
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.persistence.jpa.FavoriteRepository
import unq.pdes.backend.persistence.jpa.PurchaseRepository
import unq.pdes.backend.persistence.jpa.ReviewRepository
import unq.pdes.backend.persistence.jpa.TravelPackageRepository

@Service
class TravelPackageService(
    private val travelPackageRepository: TravelPackageRepository,
    private val hotelService: HotelService,
    private val cityService: CityService,
    private val userService: UserService,
    private val flightsClient: FlightsClient,
    private val purchaseRepository: PurchaseRepository,
    private val favoriteRepository: FavoriteRepository,
    private val reviewRepository: ReviewRepository,
) {

    @Transactional(readOnly = true)
    fun search(name: String?, origin: String?, destination: String?): List<TravelPackage> {
        return travelPackageRepository.search(
            name.blankToNull(),
            origin.blankToNull(),
            destination.blankToNull(),
        )
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): TravelPackage {
        return travelPackageRepository.findById(id)
            .orElseThrow { EntityNotFoundException("There is no TravelPackage with id: $id.") }
    }

    @Transactional(readOnly = true)
    fun findMine(username: String): List<TravelPackage> {
        val agencyUser = requireAgencyUser(username)
        return travelPackageRepository.findByAgencyId(agencyUser.agency.id!!)
    }

    @Transactional
    fun create(
        username: String,
        name: String,
        hotelId: Long,
        outboundFlightId: Long,
        returnFlightId: Long,
        price: BigDecimal,
    ): TravelPackage {
        val agencyUser = requireAgencyUser(username)
        requireUniqueName(agencyUser.agency.id!!, name, null)
        val hotel = hotelService.findById(hotelId)
        val (origin, destination) = resolveCities(hotel, outboundFlightId, returnFlightId)
        val travelPackage = TravelPackage.Builder()
            .agency(agencyUser.agency)
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
    fun update(
        username: String,
        id: Long,
        name: String,
        hotelId: Long,
        outboundFlightId: Long,
        returnFlightId: Long,
        price: BigDecimal,
    ): TravelPackage {
        val existing = findById(id)
        requireOwnedBy(username, existing)
        requireUniqueName(existing.agency.id!!, name, id)
        val hotel = hotelService.findById(hotelId)
        val (origin, destination) = resolveCities(hotel, outboundFlightId, returnFlightId)
        val travelPackage = TravelPackage.Builder()
            .id(id)
            .agency(existing.agency)
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
    fun deleteById(username: String, id: Long) {
        val travelPackage = findById(id)
        requireOwnedBy(username, travelPackage)
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

    private fun requireUniqueName(agencyId: Long, name: String, currentId: Long?) {
        val taken = if (currentId == null) {
            travelPackageRepository.existsByAgencyIdAndName(agencyId, name)
        } else {
            travelPackageRepository.existsByAgencyIdAndNameAndIdNot(agencyId, name, currentId)
        }
        require(!taken) { "The agency already has a package named '$name'." }
    }

    private fun requireOwnedBy(username: String, travelPackage: TravelPackage) {
        val agencyUser = requireAgencyUser(username)
        if (agencyUser.agency.id != travelPackage.agency.id) {
            throw AccessDeniedException("Only the owning agency can modify this package.")
        }
    }

    private fun requireAgencyUser(username: String): AgencyUser {
        val user = userService.findByUsername(username)
        return user as? AgencyUser
            ?: throw AccessDeniedException("Only agency users can manage packages.")
    }

    private fun String?.blankToNull(): String? = this?.takeIf { it.isNotBlank() }
}
