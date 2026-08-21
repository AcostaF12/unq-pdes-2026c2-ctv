package unq.pdes.backend.helpers.factory

import unq.pdes.backend.model.Destination
import unq.pdes.backend.model.Hotel

open class ObjectsFactory {

    open fun anyDestination(): Destination {
        return Destination("BUE", "Buenos Aires")
    }

    open fun destinationWith(code: String, city: String): Destination {
        return Destination(code, city)
    }

    open fun anyHotel(): Hotel {
        return hotelIn(anyDestination())
    }

    open fun hotelNamed(name: String): Hotel {
        return Hotel.Builder()
            .name(name)
            .destination(anyDestination())
            .photoUrl(SOME_PHOTO_URL)
            .build()
    }

    open fun hotelIn(destination: Destination): Hotel {
        return Hotel.Builder()
            .name("Some hotel name")
            .destination(destination)
            .photoUrl(SOME_PHOTO_URL)
            .build()
    }

    companion object {
        const val SOME_PHOTO_URL = "https://images.ctv.demo/hotels/some-hotel.jpg"
    }
}
