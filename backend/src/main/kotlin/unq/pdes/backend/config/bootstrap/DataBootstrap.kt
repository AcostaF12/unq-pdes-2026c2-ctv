package unq.pdes.backend.config.bootstrap

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import unq.pdes.backend.model.Destination
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.persistence.jpa.HotelRepository
import unq.pdes.backend.service.DestinationService
import unq.pdes.backend.service.HotelService

@Component
@Profile("dev", "prod")
class DataBootstrap(
    private val destinationService: DestinationService,
    private val hotelService: HotelService,
    private val hotelRepository: HotelRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        loadDestinations()
        loadHotels()
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
            hotel("Alvear Palace Hotel", "BUE", "https://images.ctv.demo/hotels/alvear-palace.jpg"),
            hotel("Hotel Le Meurice", "PAR", "https://images.ctv.demo/hotels/le-meurice.jpg"),
            hotel("The Savoy", "LON", "https://images.ctv.demo/hotels/the-savoy.jpg"),
            hotel("Hotel Hassler Roma", "ROM", "https://images.ctv.demo/hotels/hassler-roma.jpg"),
            hotel("Copacabana Palace", "RIO", "https://images.ctv.demo/hotels/copacabana-palace.jpg"),
        ).forEach { hotelService.save(it) }
    }

    private fun hotel(name: String, destinationCode: String, photoUrl: String): Hotel {
        return Hotel.Builder()
            .name(name)
            .destination(destinationService.findByCode(destinationCode))
            .photoUrl(photoUrl)
            .build()
    }
}
