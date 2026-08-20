package unq.pdes.backend.config.bootstrap

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.Destination
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.persistence.jpa.AgencyRepository
import unq.pdes.backend.persistence.jpa.HotelRepository
import unq.pdes.backend.persistence.jpa.UserRepository
import unq.pdes.backend.service.DestinationService
import unq.pdes.backend.service.HotelService
import unq.pdes.backend.service.UserService

@Component
@Profile("dev", "prod")
class DataBootstrap(
    private val destinationService: DestinationService,
    private val hotelService: HotelService,
    private val hotelRepository: HotelRepository,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val agencyRepository: AgencyRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        loadDestinations()
        loadHotels()
        loadUsers()
    }

    private fun loadDestinations() {
        listOf(
            Destination("BUE", "Buenos Aires"),
            Destination("PAR", "Paris"),
            Destination("LON", "London"),
            Destination("ROM", "Rome"),
            Destination("NYC", "New York"),
            Destination("TYO", "Tokyo"),
            Destination("RIO", "Rio de Janeiro"),
            Destination("BCN", "Barcelona"),
            Destination("CUN", "Cancun"),
            Destination("DXB", "Dubai"),
        ).forEach { destination ->
            if (!destinationService.existsByCode(destination.code)) {
                destinationService.save(destination)
            }
        }
    }

    private fun loadHotels() {
        if (hotelRepository.count() > 0) {
            return
        }
        listOf(
            hotel("Gran Hotel Buenos Aires", "BUE", "https://images.ctv.demo/hotels/gran-hotel-buenos-aires.jpg"),
            hotel("Hotel Palacio de París", "PAR", "https://images.ctv.demo/hotels/palacio-de-paris.jpg"),
            hotel("Hotel Real de Londres", "LON", "https://images.ctv.demo/hotels/real-de-londres.jpg"),
            hotel("Hotel Villa Roma", "ROM", "https://images.ctv.demo/hotels/villa-roma.jpg"),
            hotel("Hotel Costa de Río", "RIO", "https://images.ctv.demo/hotels/costa-de-rio.jpg"),
        ).forEach { hotelService.save(it) }
    }

    private fun hotel(name: String, destinationCode: String, photoUrl: String): Hotel {
        return Hotel.Builder()
            .name(name)
            .destination(destinationService.findByCode(destinationCode))
            .photoUrl(photoUrl)
            .build()
    }

    private fun loadUsers() {
        listOf(
            Triple("facosta", "Federico", "Acosta"),
            Triple("vferreyra", "Valentin", "Ferreyra"),
            Triple("adisanto", "Alan", "Disanto"),
        ).forEach { (username, firstName, lastName) ->
            if (!userRepository.existsByUsername(username)) {
                userService.createAdmin(username, username, firstName, lastName)
            }
        }
        if (!userRepository.existsByUsername("buyer")) {
            userService.register("buyer", "buyer123", "Bruno", "Buyer")
        }
        if (!userRepository.existsByUsername("agency")) {
            val agency = agencyRepository.findByName("Despegar")
                ?: agencyRepository.save(Agency.Builder().name("Despegar").build())
            userService.createAgencyUser("agency", "agency123", "Agus", "Agency", agency)
        }
    }
}
