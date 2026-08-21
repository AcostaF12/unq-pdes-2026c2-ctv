package unq.pdes.backend.helpers.factory

import org.springframework.stereotype.Component
import unq.pdes.backend.model.Destination
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.service.DestinationService
import unq.pdes.backend.service.HotelService

@Component
class PersistentObjectsFactory(
    private val destinationService: DestinationService,
    private val hotelService: HotelService,
) : ObjectsFactory() {

    override fun anyDestination(): Destination {
        return persist(super.anyDestination())
    }

    override fun destinationWith(code: String, city: String): Destination {
        return persist(super.destinationWith(code, city))
    }

    override fun anyHotel(): Hotel {
        return hotelService.save(super.hotelIn(anyDestination()))
    }

    override fun hotelNamed(name: String): Hotel {
        val destination = anyDestination()
        return hotelService.save(
            Hotel.Builder()
                .name(name)
                .destination(destination)
                .photoUrl(SOME_PHOTO_URL)
                .build(),
        )
    }

    override fun hotelIn(destination: Destination): Hotel {
        return hotelService.save(super.hotelIn(destination))
    }

    private fun persist(destination: Destination): Destination {
        return if (destinationService.existsByCode(destination.code)) {
            destinationService.findByCode(destination.code)
        } else {
            destinationService.save(destination)
        }
    }
}
