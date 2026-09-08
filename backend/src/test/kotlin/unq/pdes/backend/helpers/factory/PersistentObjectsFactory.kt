package unq.pdes.backend.helpers.factory

import java.math.BigDecimal
import org.springframework.stereotype.Component
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.TravelPackage
import unq.pdes.backend.model.user.AgencyUser
import unq.pdes.backend.model.user.User
import unq.pdes.backend.persistence.jpa.AgencyRepository
import unq.pdes.backend.persistence.jpa.TravelPackageRepository
import unq.pdes.backend.service.CityService
import unq.pdes.backend.service.HotelService
import unq.pdes.backend.service.UserService

@Component
class PersistentObjectsFactory(
    private val cityService: CityService,
    private val hotelService: HotelService,
    private val agencyRepository: AgencyRepository,
    private val userService: UserService,
    private val travelPackageRepository: TravelPackageRepository,
) : ObjectsFactory() {

    override fun anyCity(): City {
        return persist(super.anyCity())
    }

    override fun cityWith(code: String, name: String): City {
        return persist(super.cityWith(code, name))
    }

    override fun anyHotel(): Hotel {
        return hotelService.save(super.hotelIn(anyCity()))
    }

    override fun hotelNamed(name: String): Hotel {
        val city = anyCity()
        return hotelService.save(
            Hotel.Builder()
                .name(name)
                .city(city)
                .photoUrl(SOME_PHOTO_URL)
                .build(),
        )
    }

    override fun hotelIn(city: City): Hotel {
        return hotelService.save(super.hotelIn(city))
    }

    fun agencyNamed(name: String): Agency {
        return agencyRepository.findByName(name)
            ?: agencyRepository.save(Agency.Builder().name(name).build())
    }

    fun buyerNamed(username: String): User {
        return try {
            userService.findByUsername(username)
        } catch (_: Exception) {
            userService.register(username, "buyer123", "Bruno", "Buyer")
        }
    }

    fun agencyUserNamed(username: String, agencyName: String = "Despegar"): AgencyUser {
        return try {
            userService.findByUsername(username) as AgencyUser
        } catch (_: Exception) {
            userService.createAgencyUser(username, "agency123", "Agus", "Agency", agencyNamed(agencyName))
        }
    }

    fun packageNamed(
        name: String,
        originCode: String = "BUE",
        originCity: String = "Buenos Aires",
        destinationCode: String = "PAR",
        destinationCity: String = "Paris",
        agencyName: String = "Despegar",
    ): TravelPackage {
        val origin = cityWith(originCode, originCity)
        val destination = cityWith(destinationCode, destinationCity)
        val hotel = hotelIn(destination)
        return travelPackageRepository.save(
            TravelPackage.Builder()
                .agency(agencyNamed(agencyName))
                .hotel(hotel)
                .origin(origin)
                .destination(destination)
                .name(name)
                .outboundFlightId(1L)
                .returnFlightId(2L)
                .price(BigDecimal("1500.00"))
                .build(),
        )
    }

    private fun persist(city: City): City {
        return if (cityService.existsByCode(city.code)) {
            cityService.findByCode(city.code)
        } else {
            cityService.save(city)
        }
    }
}
